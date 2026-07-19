---
name: clover-db-format-update
description: Implementation plan to migrate Snapshot (.snapshot) and its config/value types from native Java serialization to the existing tag-based (TaggedPersistent) format
metadata:
  type: project
---

# Plan: migrate `Snapshot` and its config to the tag-based persistence format

**Date:** 2026-07-19
**Goal:** Replace the native Java serialization used by the test-optimization
`Snapshot` file (and the object graph it stores) with OpenClover's existing
tag-based, whitelist-driven persistence format (`TaggedPersistent` / `TaggedIO`),
matching how `InstrSessionSegment` already persists the registry. Related: the
same conversion should later be applied to `InstrumentationConfig`.

This is a **file-format modernization**: the tag-based format is self-describing,
versioned by an explicit tag table, forward/backward compatible in a controlled
way, and decoupled from JVM class-identity (`serialVersionUID`) churn. It also
unifies all on-disk OpenClover artifacts under a single reader/writer stack.

## 1. Background — the two persistence stacks in the codebase

OpenClover already contains a complete tag-based persistence framework, used today
for the registry's instrumentation segment:

- `org.openclover.core.io.tags.TaggedPersistent` — interface a persistable type
  implements: `void write(TaggedDataOutput out)`.
- `org.openclover.core.io.tags.ObjectReader<T>` — functional reader:
  `T read(TaggedDataInput in)`; conventionally a `static T read(TaggedDataInput)`
  on each type, registered as a method reference.
- `TaggedDataInput` / `TaggedDataOutput` — primitive + `writeUTF`, plus
  `write(Class<T>, T)` / `read(Class<T>)` and `writeList(Class<T>, List)` /
  `readList(Class<T>)` for nested `TaggedPersistent` values.
- `Tags` — the **whitelist**: maps class name ↔ numeric tag ↔ `ObjectReader`.
  Unknown tags throw `UnknownTagException`; only registered types can be
  instantiated.
- `TaggedIO` — top-level `read(FileChannel|DataInput, Tags, Class)` /
  `write(FileChannel, Tags, Class, object)` entry points.
- `TaggedInputReader` / `TaggedOutputWriter` — the concrete codecs.

Reference implementation to copy: `registry/format/InstrSessionSegment.java`
(the `TAGS` table, lines ~37-54) and `context/ContextStore.java`
(`write(TaggedDataOutput)` + `static read(TaggedDataInput)` pair).

The `Snapshot` stack, by contrast, still uses `java.io.Serializable` +
`ObjectOutputStream.writeObject` / `ObjectInputStream.readObject`
(`optimization/Snapshot.java` `store()` ~L266 and `loadFromFile()` ~L301).

## 2. Object graph to convert

All types reachable from `Snapshot` that are currently `Serializable`:

| Type | Location | Current fields (persisted state) |
|------|----------|----------------------------------|
| `Snapshot` | `optimization/Snapshot.java:39` | `cloverVersionInfo:String`, `dbVersions:Set<Long>`, `initString:String`, `testLookup:Map<String,Set<TestMethodCall>>`, `durationsForTests:Object2LongMap<TestMethodCall>`, `failingTests:Set<TestMethodCall>`, `perTestSourceStates:Map<TestMethodCall,Map<String,SourceState>>`, `avgSetupTeardownDuration:long`; `location:File` is `transient` (not persisted) |
| `TestMethodCall` | `optimization/TestMethodCall.java:11` | `runtimeTypeName:String`, `sourceMethodName:String`, `runtimeMethodName:String`, `packagePath:String` |
| `Snapshot.SourceState` | `optimization/Snapshot.java:518` (private static) | `checksum:long`, `filesize:long` |

Notes:
- `Object2LongMap` (fastutil-style) needs an explicit key/value loop; there is no
  `writeMap` helper, mirror the manual `size()` + entry loop pattern in
  `ContextStore.write`.
- `Set` / nested `Map` are serialized as an `int` count followed by elements; there
  is no generic collection tag, so encode counts by hand.
- `TestMethodCall` is used as a **map key** and in `Set`s — its `equals`/`hashCode`
  must remain intact after conversion (it is reconstructed via its private ctor in
  `read`), so keep the value semantics identical.

## 3. Design of the new format

### 3.1 Make the value types `TaggedPersistent`

For `TestMethodCall` and `SourceState`:

1. `implements TaggedPersistent`.
2. Add `public void write(TaggedDataOutput out)` writing each field with the
   matching primitive/`writeUTF` call.
3. Add `public static <Type> read(TaggedDataInput in)` reconstructing via the
   existing private constructor.
4. `SourceState` is currently a `private static` nested class — promote it to
   package-private (or keep nested) but it must be reachable for tag registration;
   give it a stable class-name key.

`TestMethodCall` example shape (mirrors `ContextStore`):

```java
public void write(TaggedDataOutput out) throws IOException {
    out.writeUTF(runtimeTypeName);
    out.writeUTF(sourceMethodName);
    out.writeUTF(runtimeMethodName == null ? "" : runtimeMethodName);
    out.writeUTF(packagePath);
}

public static TestMethodCall read(TaggedDataInput in) throws IOException {
    final String runtimeTypeName = in.readUTF();
    final String sourceMethodName = in.readUTF();
    final String runtimeMethodName = in.readUTF();
    final String packagePath = in.readUTF();
    return new TestMethodCall(runtimeTypeName, sourceMethodName, runtimeMethodName, packagePath);
}
```

(Watch nullable `String` fields — `writeUTF` rejects null; encode null as `""` or
prefix a boolean presence flag. Add a small `writeNullableUTF`/`readNullableUTF`
helper if several fields are nullable.)

### 3.2 Make `Snapshot` itself `TaggedPersistent`

`Snapshot implements TaggedPersistent`. Its `write`/`read` encode the eight
persisted fields. Collections are length-prefixed; nested `TestMethodCall` /
`SourceState` values go through `out.write(TestMethodCall.class, tmc)` /
`in.read(TestMethodCall.class)`, so the whitelist enforces types.

The `read` reconstructs a `Snapshot` through a new private constructor that takes
the decoded fields (the current constructors compute state from a
`CloverDatabase`; add a plain field-assigning ctor for load, leaving `location` to
be set by the caller as today).

### 3.3 Define the `Tags` whitelist for snapshots

Add a `SnapshotTags` table (or a `static final Tags TAGS` on `Snapshot`) modeled on
`InstrSessionSegment.TAGS`:

```java
static final Tags TAGS = new Tags()
    .registerTag(Snapshot.class.getName(),       Tags.NEXT_TAG + 0, (ObjectReader<Snapshot>) Snapshot::read)
    .registerTag(TestMethodCall.class.getName(), Tags.NEXT_TAG + 1, (ObjectReader<TestMethodCall>) TestMethodCall::read)
    .registerTag(SourceState.class.getName(),    Tags.NEXT_TAG + 2, (ObjectReader<SourceState>) SourceState::read);
```

Only these three types can ever be instantiated by the reader; anything else in the
stream fails with `UnknownTagException`.

### 3.4 Rewrite `store()` / `loadFromFile()`

Replace the `ObjectOutputStream` / `ObjectInputStream` bodies with `TaggedIO`.
Because `TaggedIO` file entry points take a `FileChannel`, open the snapshot file
via `FileChannel.open(...)`:

```java
public void store() throws IOException {
    ensureParentDirs();
    try (FileChannel ch = FileChannel.open(location.toPath(),
             WRITE, CREATE, TRUNCATE_EXISTING)) {
        TaggedIO.write(ch, TAGS, Snapshot.class, this);
    }
}

public static Snapshot loadFromFile(File file) {
    if (!(file.exists() && file.isFile() && file.canRead())) { /* existing verbose log */ return null; }
    try (FileChannel ch = FileChannel.open(file.toPath(), READ)) {
        Snapshot snapshot = TaggedIO.read(ch, TAGS, Snapshot.class);
        snapshot.location = file;
        return snapshot;
    } catch (UnknownTagException | IOException e) {
        // existing "no longer valid for this version" / "failed to load" logging
        return null;
    }
}
```

Keep the current graceful-degradation behavior (log + return `null`) so an
unreadable or old-format snapshot simply triggers a full, unoptimized run.

## 4. Backward / forward compatibility

A snapshot is a **regenerable cache** (test-optimization data), not a user
document — a stale/unreadable snapshot only costs one unoptimized test run. So:

- **No dual-format read.** The new writer emits tag format; the new reader reads
  only tag format. An old native-serialized `.snapshot` from a prior OpenClover
  version will fail to parse and be treated as "invalid for this version" (already
  the handled path via `InvalidClassException` today) — the next run regenerates it.
- Bump an explicit format marker: prepend a small `int` schema-version at the start
  of `Snapshot.write` so future field changes are detectable and can be rejected
  cleanly rather than mis-decoded.
- Optionally rename the on-disk file suffix (or add a magic header) so old and new
  files are never confused; simplest is to rely on the version marker + graceful
  fallback.

## 5. Apply the same treatment to `InstrumentationConfig`

`cfg/instr/InstrumentationConfig.java` also round-trips itself through
`ObjectInputStream`/`ObjectOutputStream` (~L349). Convert it the same way:

1. `implements TaggedPersistent` with `write` + `static read`.
2. Enumerate its persisted fields (primitives + a handful of enum/String config
   values) and encode them explicitly; enums as `writeUTF(name())` + `valueOf`.
3. Register a single-entry (or small) `Tags` table and swap the read/write helpers
   to `TaggedIO`.

This is lower-risk than `Snapshot` because the config graph is shallow and mostly
primitive/enum.

## 6. Step-by-step execution order

1. Add null-safe `writeUTF`/`readUTF` helpers if needed (small util or inline).
2. Convert `TestMethodCall` → `TaggedPersistent` (+ unit round-trip test).
3. Convert `Snapshot.SourceState` → `TaggedPersistent` (promote visibility) (+ test).
4. Add field-assigning private constructor + `write`/`read` to `Snapshot`.
5. Define the `Snapshot.TAGS` whitelist.
6. Rewrite `Snapshot.store()` / `loadFromFile()` to use `TaggedIO` + `FileChannel`.
7. Add a schema-version marker and graceful-fallback handling for old/invalid files.
8. Repeat 1-7 (scaled down) for `InstrumentationConfig`.
9. Remove `implements Serializable` / `serialVersionUID` from all converted types.

## 7. Testing

- **Round-trip unit tests** per type: build an instance, `write` to an in-memory
  `TaggedOutputWriter` over a `ByteArrayOutputStream`, `read` back via
  `TaggedInputReader`, assert deep field equality (especially `TestMethodCall`
  identity as map keys and `Set` membership).
- **End-to-end optimization test**: generate a snapshot from a real
  `CloverDatabase`, `store()`, `loadFromFile()`, and assert the optimization
  decisions (`getFailingTestPaths`, durations, per-test source states) match the
  in-memory original.
- **Old-file rejection test**: feed a legacy native-serialized `.snapshot` fixture
  and assert `loadFromFile` returns `null` with the "invalid for this version" log,
  and that the build proceeds unoptimized rather than failing.
- Run the existing `optimization` test suite to confirm no behavioral regression.

## 8. Files touched

- `optimization/Snapshot.java` (format core + `store`/`loadFromFile`)
- `optimization/TestMethodCall.java`
- `cfg/instr/InstrumentationConfig.java`
- new/extended `Tags` table(s) alongside them
- reuse (no change): `io/tags/*` (`TaggedIO`, `TaggedInputReader`,
  `TaggedOutputWriter`, `TaggedDataInput`, `TaggedDataOutput`, `Tags`,
  `TaggedPersistent`, `ObjectReader`)
- new tests under the matching `src/test` packages

Related: [[clover-db-read-rce]] tracks the coverage-segment read path
(`CoverageSegment` / `InMemPerTestCoverage`), which should receive the identical
tag-based treatment as a follow-up.
