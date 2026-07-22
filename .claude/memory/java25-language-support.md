# Java 22–25 Language Syntax Support — Report & Implementation Plan (OC-278)

## Context

OpenClover instruments Java source by parsing it with an ANTLR 2.x grammar
(`java.g` / `java-lexer.g`) and rewriting the token stream via `*Emitter` classes.
The OC-229 work (this branch) raised the instrumenter's language ceiling to **Java 21**:
`SourceLevel` now defines `JAVA_8 … JAVA_21`, `javasyntax18..21` test resources exist, and
the grammar parses/instruments the two finalized Java 21 features — **JEP 441 Pattern
Matching for switch** and **JEP 440 Record Patterns** (see `.claude/memory/java21-language-support.md`
and the `OC-229` commits on this branch).

**Goal of this iteration:** extend the instrumenter to the **finalized** language syntax added
in Java 22–25, add `SourceLevel` entries for Java 22–25, extend the IDE source-level mappers,
and add the test scaffolding mirroring the Java 18–21 work. Preview features are documented for
reference but **not implemented**.

This document doubles as the requested report. **Do not implement yet** — this is for review.

---

## Part A — Language Reference: Java 22–25 syntax changes

Only **language/syntax** JEPs are covered. Runtime/API/tooling JEPs (FFM, Vector, Stream
Gatherers, Structured Concurrency, Scoped Values, ZGC, AOT, Class-File API, KDF/PEM crypto,
JFR, Compact Object Headers, JNI, Unsafe deprecations, port removals) are **out of scope** —
they do not affect the parser.

### Summary table (finalized language syntax only)

| Java | JEP | Feature | Status | Parser/instrumenter impact |
|------|-----|---------|--------|----------------------------|
| 22 | **456** | **Unnamed Variables & Patterns** | **FINAL (mainline)** | unnamed *variables* need NO change (`_` is already an `IDENT`); only the name-only *pattern component* `Point(int x, _)` needs one new `pattern` alt |
| 23 | **467** | **Markdown Documentation Comments** | **FINAL (mainline)** | `///` comments — **none** (lexer already treats `//` as line comment) |
| 24 | — | (no finalized language feature) | — | pass-through source level only |
| 25 | **511** | **Module Import Declarations** | **FINAL (mainline)** | `import module M;` — grammar change to `importDefinition` |
| 25 | **512** | **Compact Source Files & Instance Main Methods** | **FINAL (mainline)** | implicit top-level class — grammar change to `compilationUnit` |
| 25 | **513** | **Flexible Constructor Bodies** | **FINAL (mainline)** | statements before `super()`/`this()` — likely parses; verify instrumentation |

Everything else in 22–25 is preview, incubator, or non-language (see "Out of scope").

---

### Java 22 (GA 2024-03)

**FINALIZED — must implement:**

- **JEP 456 — Unnamed Variables & Patterns.** The underscore `_` denotes an entity that is
  declared but intentionally not used. It is valid in **many** positions:
  ```java
  // unnamed pattern component in a record deconstruction (NEW SYNTAX for the parser)
  if (obj instanceof Point(int x, _)) { ... }
  switch (obj) {
      case Point(_, int y) -> ...;
      case Point(_, _)     -> ...;
  }

  // unnamed local variable / resource / catch / lambda / for — mostly already parseable
  try (var _ = acquire()) { ... }
  try { ... } catch (NumberFormatException _) { ... }
  for (var _ : list) { total++; }
  map.forEach((k, _) -> process(k));
  int _ = sideEffectingCall();
  ```
  Note: since Java 9 a lone `_` is not a legal ordinary identifier, so `_` is unambiguous here.
  `var _` uses `var` (a contextual keyword, already lexed as IDENT).

  **Real new syntax for the grammar = the bare `_` as an unnamed pattern** inside a record
  deconstruction (`Point(int x, _)`, `Point(_, _)`). All the *variable* positions (local,
  resource, catch, lambda, for) are `IDENT` in binding position and already parse — only their
  *semantics* changed, not their syntax. Type patterns with `var` binding (`case Point(var x, _)`)
  already parse for the `var x` half.

**PREVIEW — documented only, NOT implemented:**

- **JEP 447 — Statements before super(...) (Preview).** Precursor to finalized JEP 513 (Java 25).
- **JEP 459 — String Templates (2nd Preview).** `STR."...\{expr}..."`. **Later withdrawn** — do not implement.
- **JEP 463 — Implicitly Declared Classes and Instance Main Methods (2nd Preview).** Precursor to JEP 512 (Java 25).

---

### Java 23 (GA 2024-09)

**FINALIZED — trivially supported, no code change:**

- **JEP 467 — Markdown Documentation Comments.** Adds `///`-style documentation comments as an
  alternative to `/** ... */`:
  ```java
  /// A summary sentence.
  /// - a bullet in Markdown
  /// @param x the input
  int f(int x) { return x; }
  ```
  **Parser impact: none.** `///` is lexically just a `//` line comment; the Clover lexer already
  discards it as a single-line comment (comments are not tokens fed to the parser). Javadoc
  content is irrelevant to instrumentation. **HTML syntax coloring:** `///` lines are colored as
  ordinary line comments (correct enough). No action beyond a documented note + optional fixture.

**PREVIEW — documented only, NOT implemented:**

- **JEP 455 — Primitive Types in Patterns, instanceof, and switch (Preview).** `case int i ->`,
  `obj instanceof byte b`, etc. Still preview in 23/24/25 (see JEP 488/507). **Do not implement.**
- **JEP 476 — Module Import Declarations (Preview).** Precursor to finalized JEP 511 (Java 25).
- **JEP 477 — Implicitly Declared Classes and Instance Main Methods (3rd Preview).** Precursor to JEP 512.
- **JEP 482 — Flexible Constructor Bodies (2nd Preview).** Precursor to JEP 513.

---

### Java 24 (GA 2025-03)

**No finalized language features.** All language JEPs are preview iterations:

- **JEP 488** Primitive Types in Patterns (2nd Preview) — do not implement.
- **JEP 492** Flexible Constructor Bodies (3rd Preview) — precursor to JEP 513.
- **JEP 494** Module Import Declarations (2nd Preview) — precursor to JEP 511.
- **JEP 495** Simple Source Files and Instance Main Methods (4th Preview) — precursor to JEP 512.

`SourceLevel.JAVA_24` is therefore a **pure version pass-through** (same feature set as JAVA_23),
needed only so `--source 24` is accepted.

---

### Java 25 (LTS, GA 2025-09) — THREE finalized language features

**FINALIZED — must implement:**

- **JEP 511 — Module Import Declarations.** A new import form importing all packages exported by
  a module:
  ```java
  import module java.base;      // imports java.util, java.io, java.nio.*, ... transitively
  import module java.sql;
  ```
  `module` here is a **contextual keyword** (still a valid identifier elsewhere). Grammar change
  to `importDefinition` only.

- **JEP 512 — Compact Source Files and Instance Main Methods.** A compilation unit may consist of
  top-level **members** (methods and fields) with no explicit enclosing class ("compact source
  file", formerly "implicit/unnamed class"), and `main` may be a non-`static`, non-`public`
  instance method with no `String[]` parameter:
  ```java
  // whole file — no class declaration:
  String greeting = "Hello";

  void main() {
      System.out.println(greeting);
  }
  ```
  This is the **most invasive** change: `compilationUnit` currently requires a `typeDefinition`
  or `moduleDeclaration`; it must also accept a bare stream of class-body member declarations.

- **JEP 513 — Flexible Constructor Bodies.** Statements are allowed **before** an explicit
  `this(...)` / `super(...)` invocation (the "prologue"), provided they don't reference the
  instance under construction:
  ```java
  Person(String name) {
      if (name == null) throw new IllegalArgumentException();   // prologue - now legal
      this.validated = true;                                    // (field write allowed)
      super();                                                  // no longer required to be first
      // ... epilogue
  }
  ```
  Likely **already parses** in Clover (see Part B); the risk is instrumentation *placement*, not parsing.

**PREVIEW — documented only, NOT implemented:**

- **JEP 507** Primitive Types in Patterns, instanceof, and switch (3rd Preview) — do not implement.
- **JEP 502/470/505/506/508/…** — API/runtime (Stable Values, PEM, Scoped Values, Vector, …), non-language.

---

## Part B — Current state in the codebase

Recap of the OC-229 baseline plus the specific rules the new features touch. All paths under
`clover-core/src/main/java/org/openclover/core/` unless noted.

- **`cfg/instr/java/SourceLevel.java`** — enum now `JAVA_8..JAVA_21`; `JAVA_18..20` carry the
  Java-17 feature set, `JAVA_21` adds `PATTERN_MATCHING`. `fromString()` cascade descends
  `JAVA_21 → JAVA_8`. Adding 22–25 = 4 enum constants + 4 cascade branches.
- **`cfg/instr/java/LanguageFeature.java`** — `{LAMBDA, MODULES, TEXT_BLOCKS, RECORDS,
  SWITCH_EXPRESSIONS, PATTERN_MATCHING}`.
- **`instr/java/java.g`**:
  - `compilationUnit` (line ~738): `packageDefinition? importDefinition* ( moduleDeclaration |
    typeDefinition* ) EOF`. **No place for top-level members** → JEP 512 needs a new alternative.
  - `importDefinition` (line ~786): `IMPORT (STATIC)? identifierStar SEMI` → JEP 511 needs a
    `module` alternative.
  - `constructorBody` (line ~1923): `LCURLY (explicitConstructorInvocation)=>eci | /*empty*/
    (statement)* RCURLY`. The explicit `super()/this()` invocation is recognised **only if it is
    first**; entry instrumentation is placed *after* it (`endOfInv`). If a statement precedes
    `super()`, the `eci` predicate fails, `endOfInv` stays null, and `super(...)` is then parsed
    as an ordinary `statement` (it reduces through `primaryExpression`: `SUPER LPAREN argList
    RPAREN`). → **JEP 513 most likely already parses**; confirm and check instrumentation placement.
  - `caseLabelElement` / `recordPattern` / `pattern` (lines ~3689–3731): the OC-229 pattern
    hierarchy. `pattern : (typeSpec LPAREN)=>recordPattern | typeSpec IDENT`. **No `_` alt** →
    JEP 456 needs one.
  - `relationalExpression` INSTANCEOF alternatives (lines ~3261–3271) reuse `recordPattern`, so an
    unnamed pattern inside an `instanceof` record pattern is covered automatically once `pattern`
    accepts `_`.
- **`instr/java/InstanceOfState.java` / `InstanceOfStateDetector.java` / `ExpressionInfo.java`** —
  pattern-binding detection so `instanceof` patterns are not branch-instrumented. Unnamed `_`
  bindings behave exactly like named bindings here; a bare `_` pattern component is inside the
  record deconstruction which already routes to `RECORD_DECONSTRUCTION` — expected to need no
  change, but add a unit assertion.
- **`util/JavaEnvUtils.java`** — already knows `JAVA_8..JAVA_25` for **runtime** detection.
- **IDE mappers:** `clover-idea/.../jps/CloverJavaSourceTransformer.java` (explicit
  `LanguageLevel → SourceLevel` map, now up to `JDK_21 → JAVA_21`, fallback `JAVA_21`) and
  Eclipse `CloverProject.java` (string passthrough via `SourceLevel.fromString`).

Grammar is **version-agnostic** (contextual keywords via `isNextKeyword`/`isCurrentKeyword`, not
gated by `SourceLevel`), so, as with OC-229, the new rules parse regardless of `--source` and
`SourceLevel` stays a labelling/reporting concern.

---

## Part C — Implementation plan (finalized features only)

### C1. Source-level plumbing — `cfg/instr/java/SourceLevel.java`
- Add enum constants `JAVA_22, JAVA_23, JAVA_24, JAVA_25` following the `JAVA_21` pattern
  (`newHashSet("NN")`, feature set).
- Feature sets: `JAVA_22..24` keep the `JAVA_21` set (`… , PATTERN_MATCHING`); **`JAVA_25` adds
  `FLEXIBLE_CONSTRUCTORS`** (`… , PATTERN_MATCHING, FLEXIBLE_CONSTRUCTORS`) — the only new flag.
- Extend `fromString()` with `JAVA_25 → JAVA_22` branches (highest first), above `JAVA_21`.
- `autoDetect()` / `isUnsupported()` need no change.

### C2. `LanguageFeature.java` — add only if instrumentation must be gated
Parsing is version-agnostic, so **no new flag is required to parse** any of these. Recommendation:
- **Add `FLEXIBLE_CONSTRUCTORS` to `JAVA_25`'s feature set (decided).** This is the one genuinely
  level-dependent behaviour: when the feature is available (25+) Clover places the constructor entry
  `.inc()` **before** `super()/this()`; otherwise it uses the old-style placement **after**
  `super()/this()` (see C3d).
- `UNNAMED_VARIABLES` (Java 22), `MODULE_IMPORTS`, `IMPLICIT_CLASSES` (Java 25) are **not** needed —
  the grammar handles those unconditionally and safely for all levels. So `JAVA_22..24` reuse the
  `JAVA_21` set (mirroring how `JAVA_18..20` reuse the `JAVA_17` set); `JAVA_25` adds
  `FLEXIBLE_CONSTRUCTORS`.

### C3. Grammar — `instr/java/java.g` (the core work)

**C3a. JEP 456 — unnamed pattern `_` (Java 22).** Extend the `pattern` rule so a record component
may be a bare unnamed pattern:
```
pattern
    :
        (typeSpec LPAREN) => recordPattern
    |
        typeSpec IDENT
    |
        // JEP 456: an unnamed pattern component '_' (a lone underscore, IDENT in the lexer)
        { isCurrentKeyword("_") }? IDENT
    ;
```
- Lexer confirmed: `_` (`_`) is in `IdentifierStart` (java-lexer.g:488), so a lone `_`
  arrives as a plain `IDENT` with text `"_"` — there is no dedicated underscore token. The
  predicate keys off the token **text** (`{ isCurrentKeyword("_") }?` or a direct `.equals("_")`),
  not a token type.
- **Unnamed *variables* need no grammar change at all.** `_` has always lexed as `IDENT`, and
  every variable binding slot (local, resource, catch, lambda param, for-var) already accepts an
  `IDENT`. Clover only parses; javac enforces the "`_` reserved since 9 / unnamed since 22"
  semantics. So `catch (E _)`, `for (var _ : …)`, `try (var _ = …)`, `(a, _) -> …`, `int _ = …`
  already parse across all versions — add **fixtures/tests** only to lock the behaviour.
- The single reason JEP 456 needs any grammar edit is the **name-only pattern component**: a
  record component is currently `type + name` (`typeSpec IDENT`) or a nested `recordPattern`, so a
  bare `_` (name, no type) matches neither alt and fails to parse. The new alt adds that shape.
- Because `instanceof` reuses `recordPattern`/`pattern`, `obj instanceof Point(int x, _)` is
  covered by the same `pattern` change.

**C3b. JEP 511 — module import (Java 25).** Extend `importDefinition`, and **critically keep the
ordinary `import module.abc.Foo;` form working** — `module` is only a contextual keyword, so a
package/type path whose first segment is literally `module` is still legal and must still parse.
```
importDefinition
    :
        IMPORT
        (
            // JEP 511 module import: 'import module <moduleName>;'
            ( { isNextKeyword("module") }? IDENT IDENT ) =>
            IDENT n:identifier SEMI!               // consume 'module', then the (possibly dotted) module name
        |
            (STATIC)? identifierStar SEMI!         // existing: import [static] a.b.C[.*];  AND  import module.abc.Foo;
        )
    ;
```
- **The disambiguator is the token immediately after `module`**, because both forms can contain
  dots:
  - `import module java.base;` → after the `module` keyword comes an **`IDENT`** (`java`) → module
    import (JEP 511). `java.base` is the dotted *module name*.
  - `import module.abc.Foo;` → after the `module` IDENT comes a **`DOT`** → ordinary import where
    `module` is just the first path segment. Falls through to `identifierStar`.
  So the guard is "`module` keyword **and** `LA(2)` is `IDENT` (not `DOT`)". Expressed above as the
  syntactic predicate `( {isNextKeyword("module")}? IDENT IDENT ) =>` (two consecutive IDENTs =
  `module` + module-name); an equivalent `{ isNextKeyword("module") && LA(2)==IDENT }?` semantic
  predicate works too and is cheaper (bounded lookahead) — prefer that if the helper is available.
- No coverage impact — imports are not instrumented. Both forms just need to parse. Add a fixture
  covering **both** `import module java.base;` and an ordinary `import module.something.X;` to lock
  the disambiguation.

**C3c. JEP 512 — compact source files / implicit class (Java 25).** Add a top-level-members
alternative to `compilationUnit`:
```
compilationUnit
    :
        (packageDefinition)=> packageDefinition | /* nothing */
        ( importDefinition )*
        (
            (moduleDeclarationPredicate) => moduleDeclaration
        |
            // compact source file (JEP 512): top-level fields/methods, no enclosing class.
            // Disambiguate from an ordinary type: no leading class/interface/enum/record keyword.
            (compactSourceFilePredicate) => compactSourceFileMembers
        |
            ( typeDefinition[false] { topLevelClass=true; ... } )*
        )
        EOF!
    ;
```
- Introduce `compactSourceFileMembers` that reuses the existing **class body member** production
  (the same rule `typeDefinition2`/class-body uses for `field`/`method` declarations) so methods
  get the normal `MethodEntryInstrEmitter`/`instrExitMethod` treatment and fields the normal
  handling.

  **Database mapping (no new DB format).** The coverage DB is strictly
  `file → class → method → statement`, and a compact source file has no explicit class. We therefore
  **synthesize an artificial `ClassInfo`** to hold all top-level members, and register it in
  `fileInfo` exactly like a normal top-level class — every top-level method becomes a method of that
  class, every top-level field/initializer maps to that class, and statements/branches nest under
  the methods as usual. This reuses the existing structure; no schema/format change (`clover.xsd`,
  registry, `ClassInfo`/`MethodInfo` all unchanged).

  **Synthetic class name — decided: mirror `javac` (`MyFile.java → MyFile`).** `javac` compiles a
  compact source file into a class literally named after the base file name (in the unnamed package),
  so the synthetic `ClassInfo` uses **the file base name as its simple name** (`MyFile.java → MyFile`).
  This matches the *actual runtime class*, reads naturally in reports, and carries no new collision
  risk (a compact file has no explicit types, and two `MyFile.java` can't coexist in one package dir).
  Clover's recorder is index-based rather than name-based, so the name is purely a label — matching
  the real class is the most faithful choice. If reports should still flag the class as synthetic,
  prefer a `synthetic`/`implicit` flag on `ClassInfo` (or a report annotation) over mangling the name.
  Set the package to the unnamed package, ensure the name is a valid identifier (file base names may
  need light sanitising if a file name isn't a legal identifier), and give the `ClassInfo` the whole
  file span as its region. Verify `ClassInfo`/`FileInfo` requirements (qualified name, package,
  start/end region) and that reports render a class with no explicit `class` keyword without choking.
- **Disambiguation is the hard part.** A compact source file has member declarations
  (`modifiers type name ( ... ) {`) at top level, which superficially resemble the *start* of a
  type. The cleanest discriminator: it is a compact file iff the first top-level construct after
  imports is **not** a type declaration keyword (`class`/`interface`/`enum`/`@interface`/`record`)
  — i.e. it is a field or method member. Prefer a **bounded semantic predicate**
  (`looksLikeTopLevelMember()` peeking for a member signature not led by a type keyword) over an
  unbounded syntactic predicate, consistent with the OC-229 performance guidance. Because this
  adds a speculative branch to *every* file, benchmark parse time on the existing corpus.
- **Instance `main()`** itself needs no grammar work — it is an ordinary instance method; only the
  *implicit class* container is new.
- **Risk / scope note:** this is the largest and riskiest change. Consider implementing it in its
  own phase/commit, or deferring it if compact source files are judged low-value for a coverage
  tool (they are mostly used for scripts/learning). Flag for reviewer decision.

**C3d. JEP 513 — flexible constructor bodies (Java 25).**

*How constructor entry instrumentation works today (the "workaround" this JEP interacts with).*
`constructorBody` (java.g:1923) matches `LCURLY (explicitConstructorInvocation)=>endOfInv | /*empty*/
(statement)* RCURLY`, then calls `instrEnterMethod(sig, start, ct(lc), endOfInv)`. In
`instrEnterMethod` (java.g:194) the entry `RECORDER.inc()` is added as a **post-emitter on
`instrPoint = (skip==null ? leftCurly : skip)`**:
- `super()/this()` present and first → `endOfInv` = its `SEMI` → entry `.inc()` emitted **after**
  the `super()/this()` call;
- no explicit invocation → `endOfInv == null` → entry `.inc()` emitted right **after `{`**.

Placing entry coverage *after* `super()/this()` is the **pre-25 workaround**: before JEP 513 it was
illegal to put any statement (including `.inc()`) before `super()/this()`, so entry coverage had to
follow it. The `(explicitConstructorInvocation)=>` recognition is **positional** — it only fires
when `super()/this()` is the first thing in the body.

*What JEP 513 changes.* Statements may now precede `super()/this()`. Because recognition is
positional, a prologue statement makes the `explicitConstructorInvocation` predicate miss, so
`endOfInv` stays null and `super()/this()` is parsed as an **ordinary `statement`**. Consequences:
- Entry `.inc()` lands right after `{`, i.e. in the prologue. `.inc()` is a **static** call, which
  JEP 513 explicitly permits before `super()/this()` → **legal and compiles**. So flexible bodies
  are expected to *parse and instrument without any grammar change*.
- **But two behaviours become inconsistent** vs the classic form and are worth deciding:
  1. In the classic form `super()/this()` is consumed by `explicitConstructorInvocation` and is
     **not** statement-instrumented. In a flexible body it is parsed as a `statement`, so Clover
     would emit a statement `.inc()` **before** `super()/this()` and count it as a statement —
     inconsistent, and it inserts yet another prologue statement. Decide whether to suppress
     statement instrumentation of a `super()/this()` call that appears mid-body (recommended, for
     parity), or accept it.
  2. Entry coverage fires in the prologue (before `super()` runs) for flexible bodies but after
     `super()` for classic bodies. Usually harmless (we did enter the ctor), but note it.

*Decided design — gate placement on `FLEXIBLE_CONSTRUCTORS`.* Add the `FLEXIBLE_CONSTRUCTORS`
`LanguageFeature` on `JAVA_25`. In `instrEnterMethod` (or its constructor caller), choose the entry
anchor by feature:
```java
// constructor entry-instrumentation anchor
CloverToken instrPoint =
    cfg.getSourceLevel().supportsFeature(FLEXIBLE_CONSTRUCTORS)
        ? leftCurly                                  // 25+: place inc() right after '{', before super()/this()
        : (endOfInv == null ? leftCurly : endOfInv); // <25: old-style, after super()/this() (statements before it are illegal)
```
- 25+: entry `.inc()` sits in the prologue (before `super()/this()`); legal because it is a
  **static** call, which JEP 513 permits before the explicit invocation. Constructors now behave
  like normal methods (entry recorded at `{`).
- <25: unchanged — entry `.inc()` must follow `super()/this()`.
- Thread the `SourceLevel` down to this decision point if it isn't already in scope there (the cfg is
  available to the parser actions).

*Ensuring nothing is instrumented twice.* Two distinct concerns must each fire **exactly once**:
1. **Method-entry inc() vs statement inc() are different, orthogonal emitters** — both being present
   is normal (every method has an entry emitter *and* per-statement emitters); that is **not** double
   instrumentation. The rule is simply: emit the **entry emitter once**, anchored at a single point.
   On 25+ set `instrPoint = leftCurly` and pass `skip = null` so `instrEnterMethod` adds **one**
   `MethodEntryInstrEmitter` — do **not** also anchor a second one at `endOfInv`. (The pre-emitter
   `MethodRegistrationNode` stays on `leftCurly` as today.)
2. **The `super()/this()` call must be handled by exactly one grammar path.** This is already
   structurally guaranteed and must be preserved: the positional `(explicitConstructorInvocation) =>`
   branch consumes `super()/this()` **before** the `(statement)*` loop can see it, so when it fires
   the call is **not** also parsed as a statement (hence not statement-instrumented). When it misses
   (flexible body: a prologue precedes the call), `super()/this()` flows **only** through the
   `(statement)*` loop. There is no input on which the same `super()/this()` token traverses both
   paths — so it can never be instrumented twice. When moving the entry anchor on 25+, **keep the
   positional `explicitConstructorInvocation` branch** (still needed so a leading `super()/this()`
   isn't statement-instrumented and so its argument-complexity instrumentation runs) — only its role
   as the *entry anchor* is dropped; `endOfInv` is no longer consumed by `instrEnterMethod` on 25+.

*Parity note (optional).* In a flexible body where `super()/this()` is **not** first, it is parsed as
an ordinary statement and would be counted/instrumented as a statement — unlike the classic form,
where it is not. This is a single-count inconsistency, not a double-count. For parity, optionally
suppress statement instrumentation of a `this(...)`/`super(...)` invocation appearing mid-body;
otherwise accept it as counted once. Decide during implementation; not a correctness blocker.

*Actions.*
- Add the `FLEXIBLE_CONSTRUCTORS` feature (C1/C2) and the gated anchor above.
- Add fixtures/tests: a ctor with a prologue statement before `super()`, and a second ctor with a
  prologue before `this(...)`; confirm parse + compile (`--source 25`) + run, that the entry emitter
  appears **exactly once** (assert a single entry `inc()` for the ctor), and constructor coverage is
  recorded. Add a <25 regression asserting the old after-`super()` placement is unchanged.
- Confirm the instrumented output compiles when a prologue statement is itself statement-instrumented
  (static `.inc()` before it — legal).

**C3e. JEP 467 — Markdown doc comments (Java 23).** No grammar change (lexer already skips `//…`).
Only add a fixture proving a `///`-commented file instruments cleanly.

### C4. Instrumentation / complexity semantics
- **Unnamed variables/patterns (JEP 456):** `_` bindings introduce no branch and no coverage node,
  exactly like named pattern bindings — reuse the existing `InstanceOfState` /
  `RECORD_DECONSTRUCTION` handling; a `_` component must **not** be branch-instrumented. Expected
  to need no change beyond the grammar alt; add unit coverage in `InstanceOfStateTest`.
- **Module imports (JEP 511):** not instrumented; no semantics.
- **Compact source files (JEP 512):** top-level methods must get normal method entry/exit
  instrumentation and appear under the synthetic implicit class in the coverage model; verify
  method/statement/branch counting is identical to the same members inside an explicit class.
- **Flexible constructor bodies (JEP 513):** see C3d — entry instrumentation must fire exactly once
  and compile whether or not a prologue precedes `super()/this()`.
- No new `*Emitter` class is anticipated.

### C5. CLI/help surface (ceiling bump)
- `SourceLevelArg` in `cmdline/CloverInstrArgProcessors.java` uses `SourceLevel.fromString`, so
  `--source 22..25` work automatically after C1. Grep `api/command/HelpBuilder.java` and arg
  processors for any hardcoded "up to 21"/max-level text and bump to 25.

### C5b. Eclipse & IDEA plugin source-level mappers
- **Eclipse** (`clover-eclipse/.../projects/CloverProject.java`): string passthrough via
  `SourceLevel.fromString` — works for free after C1. Optional functest fixtures
  (`TestJava25Project/.settings/...prefs` with compliance=25).
- **IDEA** (`clover-idea/src/jps/java/.../CloverJavaSourceTransformer.java`): extend the explicit
  `LANGUAGE_LEVEL_TO_SOURCE_LEVEL` map and raise the fallback:
  ```java
  put(LanguageLevel.JDK_22, SourceLevel.JAVA_22);
  put(LanguageLevel.JDK_23, SourceLevel.JAVA_23);
  put(LanguageLevel.JDK_24, SourceLevel.JAVA_24);
  put(LanguageLevel.JDK_25, SourceLevel.JAVA_25);
  return sourceLevel == null ? SourceLevel.JAVA_25 : sourceLevel;
  ```
  - **Verify the bundled IntelliJ SDK actually defines `JDK_22..25`** (the plugin builds against a
    specific IntelliJ; older SDKs may lack the newest constants). Only add entries for constants
    that exist; map any `*_PREVIEW`/`JDK_X` to the highest known level via the fallback.
  - Update `CloverJavaSourceTransformerTest.java` assertions: `JDK_22..25 → JAVA_22..25`, and
    `JDK_X → JAVA_25`.

### C6. Docs / CI
- If a "supported source levels" note exists in README/docs, bump to 25.
- CI: **`.github/workflows/A-build-and-test-jdk25.yml` already exists** (alongside jdk8/11/17/21),
  so the suite already runs on a JDK that can compile `--source 25` fixtures — the D-gated
  `JavaSyntax25CompilationTest` will execute there. No new workflow needed. (There is no jdk24
  plan and none is needed — Java 24 adds no finalized language syntax, and the jdk25 job compiles
  everything through `--source 25`.)

### Files to touch (summary)
| File | Change |
|------|--------|
| `cfg/instr/java/SourceLevel.java` | add `JAVA_22..JAVA_25` + `fromString` branches |
| `cfg/instr/java/LanguageFeature.java` | add `FLEXIBLE_CONSTRUCTORS` (for `JAVA_25`) |
| `instr/java/java.g` (constructorBody / `instrEnterMethod`) | gate constructor entry anchor on `FLEXIBLE_CONSTRUCTORS`: before vs after `super()/this()` |
| `instr/java/java.g` | `pattern` `_` alt (C3a); `importDefinition` module alt (C3b); `compilationUnit` compact-file alt (C3c) |
| `instr/java/InstanceOfState*.java`, `ExpressionInfo.java` | verify `_` bindings; likely no change |
| `cmdline/CloverInstrArgProcessors.java`, `api/command/HelpBuilder.java` | help/validation ceiling → 25 |
| `clover-idea/.../CloverJavaSourceTransformer.java` (+ test) | map `JDK_22..25 → JAVA_22..25` |
| tests + resources | see Part D |

---

## Part D — Test plan (what to write)

Follow the Java 18–21 pattern exactly.

### D1. Compilation tests (file-level, end-to-end)
Gate every JDK-version-specific test with
`assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_NN))`.

- **`JavaSyntax22CompilationTest.groovy`** (srcDir `javasyntax22`):
  - `testUnnamedPatternInRecordDeconstruction` — `case Point(int x, _)` / `Point(_, _)`: the file
    instruments, compiles, runs; the `_` component adds no branch (assert-negative on branch regex).
  - `testUnnamedPatternInInstanceof` — `if (obj instanceof Point(int x, _))`.
  - `testUnnamedLocalsAndCatchAndFor` — `int _ = f();`, `catch (Exception _)`, `for (var _ : list)`,
    `try (var _ = res())`, `(a, _) -> a` — all parse, instrument, run.
- **`JavaSyntax23CompilationTest.groovy`** (srcDir `javasyntax23`):
  - `testMarkdownDocComments` — a file using `///` doc comments instruments cleanly and runs
    (proves the lexer skips them; guards against regressions).
  - `testSourceLevelAcceptedAndInstruments` — thin `--source 23` wiring check.
- **`JavaSyntax24CompilationTest.groovy`** (srcDir `javasyntax24`):
  - `testSourceLevelAcceptedAndInstruments` — thin `--source 24` wiring check (no new syntax).
- **`JavaSyntax25CompilationTest.groovy`** (srcDir `javasyntax25`):
  - `testModuleImportDeclaration` — `import module java.base;` (JEP 511) parses; the rest of the
    file instruments and runs.
  - `testOrdinaryImportWithModulePrefix` — regression: `import module.something.X;` (ordinary import
    whose first path segment is the word `module`) still parses correctly and is **not** mistaken for
    a module import — locks the C3b `module`-then-IDENT-vs-DOT disambiguation.
  - `testFlexibleConstructorBody` — a constructor with a prologue statement before `super()` and a
    second ctor with a prologue before `this(...)`; assert entry instrumentation fires once, the
    instrumented source compiles and runs, and constructor coverage is recorded.
  - `testCompactSourceFileWithInstanceMain` — a compact source file (top-level `void main()` +
    top-level field/method), assert it instruments, compiles (`--source 25`), runs, and that the
    top-level methods appear in the coverage model (under the synthetic implicit class).
    *(If C3c is deferred, mark this `@Ignore` with a TODO referencing the decision.)*
  - `testSourceLevelAcceptedAndInstruments` — thin `--source 25` wiring check.

Also add a **`SourceLevel.fromString` unit assertion** (extend `SourceLevelTest.groovy`) that
`"22"/"23"/"24"/"25"` resolve to the matching enum constants.

**JDK gating caveat:** compact source files, module imports, flexible ctor bodies, and unnamed
patterns require a **JDK 22/25** to *compile* the fixtures. The `assumeTrue(... isAtLeastJavaVersion
...)` gate skips them on older JDKs (as the Java 21 tests already do), so they only actually run in
the JDK 25 CI job — which already exists (`A-build-and-test-jdk25.yml`); no CI change required.

### D2. Resource fixtures
- `clover-core/src/test/resources/javasyntax22/` — `Java22UnnamedPatterns.java`,
  `Java22UnnamedVariables.java`.
- `clover-core/src/test/resources/javasyntax23/` — `Java23MarkdownDoc.java`, `Java23Simple.java`.
- `clover-core/src/test/resources/javasyntax24/` — `Java24Simple.java`.
- `clover-core/src/test/resources/javasyntax25/` — `Java25ModuleImport.java`,
  `Java25FlexibleConstructor.java`, `Java25CompactSourceFile.java`.
- Each `main` (or the compact file's top-level `main`) prints observable output so
  `assertExecOutputContains` verifies runtime coverage, matching the Java 18–21 style.

### D3. Unit-level instrumentation tests (`instr/java/`)
- Extend `InstanceOfStateTest.groovy` with an unnamed-component record pattern
  (`obj instanceof Point(int x, _)`) asserting `RECORD_DECONSTRUCTION` and no branch on `_`.
- If C3c lands, add an instrumentation test asserting top-level methods of a compact source file
  receive method entry/exit instrumentation.

### D4. Regression guard
- Confirm all existing `JavaSyntax3..21` tests still pass — the `pattern`, `importDefinition`, and
  `compilationUnit` changes must be strictly additive. In particular re-run
  `InstrumentationSwitchStatementsTest`, `InstanceOfStateTest`, and the full `JavaSyntax*`
  compilation suite before/after each grammar edit (the `compilationUnit` and `importDefinition`
  edits touch hot, universal rules).

---

## Part E — Cross-module impact & report generation

### E1. `clover-ant` — thin passthrough, no code change
Source level flows straight through `SourceLevel.fromString` (`AbstractInstrTask`,
`CloverCompilerAdapter`). No code change once C1 lands; optional Java 25 ant functest fixture.

### E2. `clover-groovy` — NOT impacted
Instruments Groovy via Groovy's own AST, not `java.g`. Java 22–25 (a Java-grammar change) is
orthogonal. No change; confirm its suite still passes on the CI JDK.

### E3. Report STRUCTURE (XML & HTML) — unaffected, no schema change
Coverage is modelled per line as statement / branch / method, never per syntactic construct. None
of the finalized 22–25 features introduce a new coverage node type:
- unnamed `_` bindings: no node (like any pattern binding);
- module imports: not instrumented;
- compact source files: ordinary `stmt`/`method` lines, just under the **synthetic `ClassInfo`**
  from C3c (named after the file base, unnamed package) — this is the only structural novelty, and
  it reuses the existing `file → class → method → statement` shape with **no new DB format**.
  **Verify the synthetic class serialises cleanly** into the DB/XML (name, package, region) and
  satisfies `clover.xsd` `<class>`/`<file>` expectations. No schema element changes expected;
- flexible ctor bodies: ordinary method/stmt lines.
No `clover.xsd` bump anticipated. The one thing to check end-to-end is the implicit-class container
(E3 + D1 `testCompactSourceFileWithInstanceMain`).

### E4. HTML syntax COLORING — cosmetic, low priority
The HTML source view runs an independent `JavaLexer` pass (`JavaTokenTraverser`, rendered by
`src-file-source-lines.vm`; keyword set in `JavaKeywords.java`). The finalized 22–25 features
introduce **no new tokens** — `_` is an `IDENT`, `module` is a contextual keyword (IDENT), `///`
is a line comment, `super()/this()` are existing tokens. So the highlighter already tolerates the
new syntax without breaking. Contextual keywords (`module`, `when`, `var`, `record`, `sealed`)
render as identifiers, consistent with current behaviour. **Recommend leaving coloring as-is.**
`///` doc comments render as ordinary comments (acceptable). Optional polish only.

### E5. Report-side tests
No report-side test changes required unless E4 polish is pursued. If C3c lands, add one
`XMLReporter`/model assertion (or reuse the compilation test's DB assertions) that a compact source
file's implicit class appears with the expected members.

---

## Verification

1. Build with a JDK that compiles `--source 25` (JDK 25). Run **clover-core** tests focusing on the
   new `JavaSyntax22/23/24/25CompilationTest`, `SourceLevelTest`, `InstanceOfStateTest`,
   `InstrumentationSwitchStatementsTest`, plus the regression `JavaSyntax3..21` suite.
2. The compilation tests are the real end-to-end check: instrument via `CloverInstr.mainImpl`,
   compile with forked `Javac` at `--source NN`, run the main class, and assert both the
   instrumented source (`assertFileMatches`) and the coverage DB — so a green
   `JavaSyntax25CompilationTest` proves parse + instrument + compile + run + coverage.
3. **clover-idea:** run `CloverJavaSourceTransformerTest` (updated per C5b) — `JDK_22..25 → JAVA_22..25`.
4. **clover-ant & clover-groovy:** run both suites to confirm no regression.
5. **Report generation:** generate HTML + XML over a project with a compact source file, a module
   import, a flexible ctor body, and an unnamed pattern; confirm the XML validates against
   `clover.xsd` unchanged and the implicit class serialises with members.
6. Manual smoke: `--source 25` on a compact source file with `void main()`; confirm no parse error
   and coverage of the top-level method.
7. **Parser performance:** benchmark parse time of the existing corpus before/after the
   `compilationUnit` (C3c) and `importDefinition` (C3b) edits — both are on universal hot paths;
   the compact-file speculative branch runs on every file. Prefer bounded semantic predicates.

## Suggested phasing (mirrors the OC-229 two-phase workflow)

Because the risk profile is very uneven, suggest splitting implementation into reviewable phases:

- **Phase 1 — plumbing + low-risk syntax:** `SourceLevel` 22–25, IDE mappers, CLI ceiling,
  JEP 456 unnamed patterns/vars (C3a), JEP 511 module imports (C3b), JEP 467 fixture (C3e), and all
  thin wiring + regression tests. Small, additive, low blast radius.
- **Phase 2 — flexible constructor bodies (JEP 513, C3d):** likely verify-only; add tests, fix
  instrumentation placement only if a real problem surfaces.
- **Phase 3 — compact source files / implicit classes (JEP 512, C3c):** the invasive
  `compilationUnit` change + synthetic implicit-class modelling. Isolated so its parser-performance
  and report-model impact can be reviewed on its own — or deferred entirely if judged low-value.

## Out of scope (documented, not implemented)
- **String Templates** (JEP 430 Java 21 / 459 Java 22) — preview, **withdrawn** in later JDKs.
- **Primitive Types in Patterns, instanceof, switch** (JEP 455/488/507) — still preview through 25.
- **Statements before super / Flexible Constructor Bodies preview iterations** (JEP 447/482/492) —
  superseded by finalized JEP 513, which IS in scope.
- **Module Import Declarations preview iterations** (JEP 476/494) — superseded by finalized JEP 511.
- **Implicitly Declared/Simple/Compact Source Files preview iterations** (JEP 463/477/495) —
  superseded by finalized JEP 512.
- All API/runtime/tooling JEPs (FFM, Vector, Stream Gatherers, Structured Concurrency, Scoped
  Values, Class-File API, AOT, JFR, crypto, GC, port removals).
</content>
</invoke>
