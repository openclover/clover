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
`ObjectInputStream`/`ObjectOutputStream` (`saveToFile` ~L338 / `loadFromStream`
~L348).

**Investigation findings (2026-07-19) — the plan's original "shallow, mostly
primitive/enum, lower-risk" framing was wrong.** What was learned:

- The object actually serialized is an **`AntInstrumentationConfig`** (in
  `clover-ant`), written by `ant/groovy/GroovycSupport.newConfigDir` →
  `saveToFile`. It is read back in a **different module** (`clover-groovy`,
  `CloverAstTransformerBase.loadConfig`) as a base `InstrumentationConfig`. So
  native serialization is relied on for **cross-module polymorphic** read.
- Class hierarchy: `AntInstrumentationConfig extends JavaInstrumentationConfig
  extends InstrumentationConfig`. The subclasses add:
  - **Persisted scalars** — Java: `sourceLevel`, `fullyQualifiedJavaNames`,
    `instrFileExtension`, `instrumentLambda`, `sourceDir`, `destDir`,
    `sourceFiles`; Ant: `preserve`, `compilerDelegate`, `groverJar`,
    `skipGroverJar`.
  - **`transient` Ant-runtime state** — `Project`, `List<FileSet>`,
    `List<TestSourceSet>`, `PatternSet` (never serialized) + Ant-coupled behavior
    (`resolveInitString` override, `EnumeratedAttribute` nested helpers,
    `addConfiguredFileSet`, `configureIncludedFiles`).
- **The `clover-groovy` reader consumes ONLY base-class accessors** (verified):
  `getEncoding/getFlushPolicy/getFlushInterval/getIncludedFiles/getInitString/
  getProfiles/getProjectName/getRegistryFile/getTestDetector/getTmpDir/
  getDistributedConfigString/isEnabled/isIntervalBasedFlushing/
  isRecordTestResults/isSliceRecording/isStatementInstrEnabled`. No
  subclass-specific field is ever read back. The only `saveToFile` caller is
  `GroovycSupport`; the only `loadFromStream` caller is the Groovy transformer.

### 5.1 DESIGN DECISION — flatten persistence to the base class

Because (a) no Ant-typed field is persisted (all `transient`) and (b) the reader
only reads base-class state, **persistence flattens to the single base
`InstrumentationConfig` type**:

- Make **`InstrumentationConfig implements TaggedPersistent`**, `write`ing only its
  own fields; register **one** tag for it. The Ant/Java subclasses inherit
  `write()` and are **not** registered — no discriminator, no cross-module
  polymorphism, no Ant dependency dragged into `clover-core`.
- On read, reconstruct a plain `InstrumentationConfig` carrying exactly the state
  the Groovy side uses. This realizes the "one class" intuition at the persistence
  layer rather than by physically collapsing the classes (which is blocked — the
  Ant runtime members require `org.apache.tools.ant.*`, unavailable in
  `clover-core`).
- Encode scalars directly; enums as `writeUTF(name())` + `valueOf`; nullable
  strings are fine (`TaggedOutputWriter.writeUTF` already handles null).
- **`DistributedConfig`** → persist as its **string form** (has
  `DistributedConfig(String)` ctor + `toString()`); no dedicated tag needed.

### 5.2 DESIGN DECISION — persist the RESOLVED `TestDetector`, reinstantiate on read

`getTestDetector()` **is** consumed by the Groovy side and must round-trip. The
detector is a graph, but by serialization time it has already been **resolved**
(Ant `FileSet`/`TestSourceSet`/`BooleanSpec`/`AndSpec`/`OrSpec`/`TestClassSpec`
builders are consumed during `buildTestDetector()` + directory scan). So we
persist only the resolved data and **reinstantiate the concrete class on read**
(a per-type tag; each type writes its own fields). Types + fields:

| Persisted type | Fields | Reinstantiate as |
|---|---|---|
| `NoTestDetector` | none | `new NoTestDetector()` |
| `DefaultTestDetector` | none | `new DefaultTestDetector()` |
| `TestSpec` | 9 regex **strings** (`pkg`, `class`, `classAnnotation`, `super`, `classTag`, `method`, `methodAnnotation`, `methodReturnType`, `methodTag`), each nullable | `new TestSpec()` + setters (`Pattern.compile` on read) |
| `AggregateTestDetector` | strategy discriminator (`And`/`Or`) + child detector list | `new AggregateTestDetector(strategy)` + `addDetector` |
| `FileMappedTestDetector` | matcher list + `defaultDetector` | `new FileMappedTestDetector()` |
| test-source matcher | resolved `Set<File> includedFiles` + its `TestDetector` | core matcher (see 5.3) |

The `BooleanSpec` builder graph and Ant `FileSet` machinery are **not** persisted.

### 5.3 DESIGN DECISION — matcher wrinkle → new core `SimpleTestSourceMatcher`

`FileMappedTestDetector.testFileMatchers` currently holds Ant `TestSourceSet`
(in `clover-ant`) instances, and the `TestSourceMatcher` interface exposes only
`matchesFile(f)`/`getDetector()` — not the resolved file set. The `Tags` table +
`write()` live in `clover-core` and cannot reference the Ant class.

**Chosen (option 1):** introduce a core
`instr/tests/SimpleTestSourceMatcher implements TestSourceMatcher,
TaggedPersistent` holding `Set<File> includedFiles` + `TestDetector detector`, and
change `GroovycSupport` to add a `SimpleTestSourceMatcher` (built from the
`TestSourceSet`'s already-resolved `getIncludedFiles()` + `getDetector()`) to the
`FileMappedTestDetector` instead of the raw Ant `TestSourceSet`. Keeps all
persistence in `clover-core`; small, contained behavior change in `GroovycSupport`.
(Rejected option 2: widen `TestSourceMatcher` with `getIncludedFiles()` + convert
at `write()` time — avoids the `GroovycSupport` change but grows the interface and
does type-narrowing in the codec.)

### 5.4 Tag table for the config

One `Tags` table (on `InstrumentationConfig`) registering: `InstrumentationConfig`,
`NoTestDetector`, `DefaultTestDetector`, `TestSpec`, `AggregateTestDetector`,
`FileMappedTestDetector`, `SimpleTestSourceMatcher`. `TestDetector` fields are
written via `out.write(TestDetector.class, detector)` so the whitelist enforces the
allowed concrete types.

### 5.5 Files touched for step 8

- `cfg/instr/InstrumentationConfig.java` — `TaggedPersistent`, `TAGS`,
  `saveToFile`/`loadFromStream` → `TaggedIO`.
- `instr/tests/{TestSpec,AggregateTestDetector,FileMappedTestDetector,
  NoTestDetector,DefaultTestDetector}.java` — `TaggedPersistent` + `read`.
- **new** `instr/tests/SimpleTestSourceMatcher.java`.
- `ant/groovy/GroovycSupport.java` — build `SimpleTestSourceMatcher` instead of
  adding the Ant `TestSourceSet` as the matcher.
- Remove `implements Serializable`/`serialVersionUID` from converted types.

## 6. Step-by-step execution order

**Snapshot conversion — DONE (2026-07-19), all `optimization` tests green:**

1. ~~null-safe UTF helpers~~ — not needed; `TaggedOutputWriter.writeUTF` already
   handles null.
2. ✅ `TestMethodCall` → `TaggedPersistent` (`write` persists 3 fields;
   `runtimeMethodName` is derived by the 3-arg ctor, not stored).
3. ✅ `Snapshot.SourceState` → `TaggedPersistent` (promoted `private`→package).
4. ✅ Field-assigning private ctor + `write`/`read` on `Snapshot`
   (`durationsForTests` custom-default map rebuilt via `newDurationsMap()`).
5. ✅ `Snapshot.TAGS` whitelist (`Snapshot`, `TestMethodCall`, `SourceState`).
6. ✅ `store()`/`loadFromFile()` use `TaggedIO` + `FileChannel`
   (`WRITE,CREATE,TRUNCATE_EXISTING` / `READ`).
7. ✅ `FORMAT_VERSION` marker (int, first field) + graceful fallback: old
   native-serialized files hit `UnknownTagException` (magic `0xAC` byte is not a
   valid tag) → "no longer valid for this version" warn + `null`.
9. ✅ Removed `implements Serializable` / `serialVersionUID` from the three types.

**InstrumentationConfig conversion — DONE (2026-07-19), all relevant tests green:**

8. ✅ Per §5.1–5.5: base-flatten `InstrumentationConfig` (`TaggedPersistent`,
   `CONFIG_FORMAT_VERSION = 50001`, `TAGS` at `NEXT_TAG + 50..56`,
   `saveToFile`/`loadFromStream` → `TaggedIO`; scalars/strings/enums inline,
   `DistributedConfig`/`CloverProfile`/`MethodContextDef`/`StatementContextDef`
   inline, `TestDetector` via the whitelist). New `SimpleTestSourceMatcher`;
   `GroovycSupport` now wraps each resolved `TestSourceSet` into one.
   `NoTestDetector`/`DefaultTestDetector`/`TestSpec`/`AggregateTestDetector`/
   `FileMappedTestDetector` made `TaggedPersistent`; `TestDetector`/
   `TestSourceMatcher` interfaces now extend `TaggedPersistent` with a default
   `write` that throws (non-persistable impls fail fast).

   **Key implementation gotcha:** `TaggedOutputWriter.write(Class,obj)` keys the
   on-disk tag off the **declared Class argument, not the runtime type** — so
   `out.write(TestDetector.class, d)` fails with `UnknownTagException` (only
   concrete types are registered). Writing polymorphic detectors requires
   dispatching to the concrete class; centralized in new
   `instr/tests/TestDetectorIO.{writeDetector,writeMatcher}`. Reading stays
   polymorphic (`in.read(TestDetector.class)` — the stream tag selects the reader).
   `AggregateTestDetector` strategy stored by simple-class-name string (future-proof
   for new `BooleanStrategy` impls), not a boolean.

**Format-version bumps (defensive, so new OpenClover rejects old data):**

- `RegHeader.REG_FORMAT_VERSION` 40502 → **50001** (drives `BaseCoverageRecording`
  too) — `.db` registry + coverage recordings.
- `Snapshot.SNAPSHOT_FORMAT_VERSION` = **50001** (renamed from `FORMAT_VERSION`).
- `InstrumentationConfig.CONFIG_FORMAT_VERSION` = **50001**.
- Distinct tag ranges **per file format** to fail fast on wrong-table reads:
  `clover.db` (`InstrSessionSegment`) `NEXT_TAG+0..`, config stream `NEXT_TAG+50..`,
  `.snapshot` `NEXT_TAG+100..`. Note the distinct-range rule is *cross-file*; within
  one file the segments should share a single coherent namespace — so the pending
  coverage-segment conversion (§9) continues the `.db` numbering (`+16..`) rather
  than opening a new range.

**Tests added:** `instr/tests/TestDetectorPersistenceTest.groovy` (per-type
round-trip for every new `TaggedPersistent` detector/matcher);
`InstrumentationConfigSerializationTest.groovy` expanded to full save/load
round-trips (scalars, nullables, `DistributedConfig`, context defs, profiles, and
an **Ant→Groovy-shaped** `FileMappedTestDetector`+`SimpleTestSourceMatcher` graph);
`SnapshotTest.groovy` +2 (failing-test/lookup reload identity; corrupt/old-file →
null).

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

## 9. Coverage segment: `CoverageSegment` / `InMemPerTestCoverage` (PENDING)

The last on-disk artifact still using native Java serialization. Converting it to
the tag-based format brings the whole `clover.db` under one reader/writer stack and
makes the coverage read path self-describing and whitelist-driven, consistent with
`InstrSessionSegment`.

### 9.1 Where the native serialization lives

`registry/format/CoverageSegment.java` stores two sub-blocks inside the `.db`
coverage segment (footer at `CoverageSegment.Footer`, marker `0xb4b00`):

- **hit counts** — an `int[]`, already written/read as raw bytes via `BufferUtils`
  (`loadHitCounts` / `write`). **Safe; leave as-is.**
- **per-test coverage** — `InMemPerTestCoverage`, written with
  `ObjectOutputStream.writeObject` (`write` ~L155-160) and read with
  `ObjectInputStream.readObject` (`loadPerTestCoverage` ~L98-107). **This is the
  block to replace.**

Only the per-test sub-block changes; the footer, the two `LazyLoader`s, and the
hit-counts path stay. `CoverageSegment` already works channel-based (like
`InstrSessionSegment`), so the perTest block just switches from `Object*Stream` to
`TaggedIO.read/write(channel, TAGS, InMemPerTestCoverage.class[, obj])`. The tag
stream is self-delimiting (length-prefixed counts, as `ContextStore` in
`InstrSessionSegment`), so the `LazyLoader` positioned at the block start reads
without over-running into the hit-counts region.

### 9.2 Object graph to convert

| Type | Location | Persisted state |
|------|----------|-----------------|
| `InMemPerTestCoverage` | `recorder/InMemPerTestCoverage.java:28` | `coverageSize:int` (from `BasePerTestCoverage`); `tciToHits: LinkedHashMap<TestCaseInfo,BitSet>` (**insertion order matters**). `tciIDToTCIMap` + `uniqueCoverageMask` are `transient`, rebuilt on read (`rebuildTCIIDMap()` in the current `readObject`) |
| `FullTestCaseInfo` | `registry/entities/FullTestCaseInfo.java:23` | non-transient: `runtimeTypeName`, `sourceMethodName`, `hasResult`, `startTime`, `endTime`, `duration`, `error`, `failure`, `failMessage`, `failType`, `failFullMessage`, `staticTestName`, `runtimeTestName`, `hashCode:Integer` (nullable). `transient`: `sourceMethod`/`runtimeType` (`WeakReference`s), `id:Integer`, `stackTrace` (rebuilt from `failFullMessage`) |
| `BitSet` (value) | JDK | encode as `long[]` via `BitSet.toLongArray()` + length prefix; rebuild with `BitSet.valueOf(long[])` (no tag needed — inline) |

`TestCaseInfo` is the map-key interface; the only concrete persisted impl is
`FullTestCaseInfo`. As with `TestDetector` (§5.2), make `TestCaseInfo` (or just the
codec helper) route writes through the concrete class — recall the write-side tag is
keyed off the **declared `Class`** (§6 gotcha), so write `FullTestCaseInfo.class`
explicitly.

### 9.3 Design (mirror §5 / Snapshot)

1. `InMemPerTestCoverage implements TaggedPersistent`:
   - `write`: `writeInt(coverageSize)`, then `writeInt(tciToHits.size())` and, **in
     LinkedHashMap iteration order**, for each entry `out.write(FullTestCaseInfo.class,
     tci)` followed by the `BitSet` as `long[]` (`writeInt(len)` + `writeLong` loop).
   - `static read`: rebuild the `LinkedHashMap` in order, then call the existing
     `rebuildTCIIDMap()` post-processing (add a field-assigning/rebuild path since the
     current logic lives in `readObject`). Reconstruct via a load-oriented ctor.
2. `FullTestCaseInfo implements TaggedPersistent` with `write` + `static read` over
   the non-transient fields; on read, leave `sourceMethod`/`runtimeType`/`stackTrace`
   to be lazily rebuilt exactly as `readObject` does today (`stackTrace` from
   `failFullMessage`). **Watch the `id`/slice-offset logic:** `id` is `transient` and
   assigned from the static `instanceCache`/`sliceOffset`; `tciIDToTCIMap` keys on
   `tci.getId()`, so `read` must preserve the current id-assignment semantics
   (verify `getId()` lazy assignment + `rebuildTCIIDMap()` ordering — the one real
   subtlety in this conversion).
3. Coverage tag whitelist — register `InMemPerTestCoverage` + `FullTestCaseInfo`.
   **Because the coverage segment lives in the same `clover.db` as
   `InstrSessionSegment`, keep one coherent tag namespace for the file**: do NOT
   invent a far-off range — either share a single `.db`-wide `Tags` table with
   `InstrSessionSegment`, or continue its numbering by appending the coverage types
   at `NEXT_TAG + 16..` (its table currently ends at `+15`). Numeric collision is not
   a correctness issue (the two segments are separate tag streams), but a single
   namespace means a tag byte means the same thing everywhere in the `.db`.
   Preferred: a shared table (e.g. lift `InstrSessionSegment.TAGS` to a common
   `registry/format` constant registering both segments' types) so the whole file has
   one registry. Distinct ranges are reserved for the genuinely separate *file*
   formats (config `+50`, `.snapshot` `+100`), where reading with the wrong table
   should fail fast.
4. Rewrite `CoverageSegment.loadPerTestCoverage`/`write` to use `TaggedIO` over the
   channel; keep the `Footer`/byte-length bookkeeping unchanged.
5. Remove `implements Serializable`/`serialVersionUID`/`readObject` from
   `InMemPerTestCoverage`, `BasePerTestCoverage`, `FullTestCaseInfo` once converted.

### 9.4 Compatibility & versioning

The coverage segment lives in the `.db`, already guarded by
`RegHeader.REG_FORMAT_VERSION` (bumped to **50001**), so old/new `.db` files are
already rejected cleanly with "Please regenerate" — **no separate per-segment
version marker needed** (unlike the standalone `.snapshot`/config files). An old
`.db` never reaches the coverage reader because the header check fails first.

### 9.5 Files touched

- `registry/format/CoverageSegment.java` — `TAGS` + `TaggedIO` for the perTest block.
- `recorder/InMemPerTestCoverage.java` — `TaggedPersistent` + `read` + rebuild.
- `recorder/BasePerTestCoverage.java` — drop `Serializable`/`serialVersionUID`.
- `registry/entities/FullTestCaseInfo.java` — `TaggedPersistent` + `read`.
- new tests: round-trip `InMemPerTestCoverage` (incl. `BitSet` fidelity, tci ordering,
  id/`getTestById` after reload) + an invalid-tag rejection test.

### 9.6 Testing

- **Round-trip**: build an `InMemPerTestCoverage` with several `FullTestCaseInfo`
  keys and non-trivial `BitSet`s, write via `TaggedOutputWriter`/read via
  `TaggedInputReader`, assert `getTests()`, `getHitsFor(tci)`, `getTestById(id)`,
  `hasPerTestData()`, and unique-mask (`initMasks`) all match the original.
- **End-to-end**: save a `Clover2Registry` with coverage, reload via
  `Clover2Registry.fromFile`, assert per-test coverage identical.
- **Robustness**: feed a coverage segment containing a non-whitelisted tag and
  assert it fails cleanly (`UnknownTagException` / `CorruptedRegistryException`).
