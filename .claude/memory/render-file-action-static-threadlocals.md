# RenderFileAction NPE — static ThreadLocals (issue #122)

## Status

Confirmed still reproducible on 5.0.0 / 5.0.1-SNAPSHOT. Unfixed as of 2026-07-17.

## Issue #122 holds two unrelated bugs

Worth splitting before any fix work:

1. **The original report** (title + linked `bmuschko/gradle-clover-plugin#146`) — a JFreeChart NPE at `ValueAxis.findMaximumTickLabelWidth`, i.e. headless-AWT/font-metrics on macOS Catalina. Not investigated.
2. **The commenters' NPE** (`RenderFileAction.call:111`) — the bug described below. This is what most people in the thread are actually hitting.

## Root cause

`clover-core/src/main/java/org/openclover/core/reporters/html/RenderFileAction.java` keeps
per-thread render state in **static** fields:

```java
protected static ThreadLocal<List<Column>> columnsTL;
protected static ThreadLocal<ContextSet> contextSetTL;
```

Their lifecycle is owned by `HtmlReporter` (`initThreadLocals()` at `HtmlReporter.java:269`,
`resetThreadLocals()` in a `finally` at `:308`), and the reset assigns the statics back to `null`
rather than calling `ThreadLocal.remove()`. `RenderFileAction.call()` then dereferences them —
`if (columnsTL.get() == null)`, which is **line 111 at the `ant-prod-4.4.1` tag** and matches the
reported stack traces exactly.

Because the fields are static rather than per-reporter, two paths reach the NPE:

- **Two reports in one JVM.** Gradle daemon / parallel Maven: report B's `finally` nulls the
  statics out from under report A while A is still rendering. Explains the recurring
  "fails on CI, works locally" pattern — CI reuses long-lived daemon JVMs.
- **Any exception mid-render.** `service.shutdown()` / `awaitTermination()` sit *inside* the
  `try`, so an earlier failure jumps to `finally`, resets the statics, and the queued tasks throw
  NPEs that bury the original cause. Likely explains the "only when we run partial tests" report.

## Why it's easy to miss

`CloverExecutors$LoggingCallable` catches `Throwable` and only **warns** — the bare `null` line
preceding each trace in bug reports is the NPE's null message being logged. The report does not
fail; the affected pages are just silently missing.

## Not a Jenkins plugin bug

The Jenkins Clover Plugin never invokes `HtmlReporter` — it only imports `org.openclover.ci.*` +
`ClassPathUtil`, parses `clover.xml`, and copies the already-generated HTML directory. The NPE
happens in the Maven/Ant JVM upstream of the plugin, so the plugin version is irrelevant and
`hpi:run` will not reproduce it.

## Reproduction

`clover-core/src/test/groovy/org/openclover/core/reporters/html/RenderFileActionConcurrencyTest.groovy`
(**uncommitted** as of writing — left for review).

```
mvn -pl clover-core -am test -Dtest=RenderFileActionConcurrencyTest -DfailIfNoTests=false
```

Note `-am` is required: sibling modules (`clover-runtime`, `clover-buildutil`) aren't in `.m2` as
5.0.1-SNAPSHOT, so `-pl clover-core` alone fails dependency resolution.

## Fix direction

Make the state per-report rather than static, so each `HtmlReporter` owns its own lifecycle and one
report's cleanup cannot touch another's in-flight tasks. Also ensure the `finally` reset cannot run
while tasks may still be executing (`shutdown()`/`awaitTermination()` should complete first).
