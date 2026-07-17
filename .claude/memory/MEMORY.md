# Memory Index

- [Non-instrumentable coverage state](non-instrumentable-coverage-state.md) — Idea: add NON_INSTRUMENTABLE state for try-with-resources resources and similar injection-impossible sites
- [Groovy 4 Language Instrumentation Plan](groovy4-language-instrumentation.md) — OC-192: implementation plan for Groovy 4 support; switch expressions, records, sealed classes, GINQ
- [Groovy 5 Language Instrumentation Plan](groovy5-language-instrumentation.md) — Groovy 5
- [Eclipse Libs Installer Plan](eclipse-libs-installers.md) — Plan for processing all Eclipse versions (Luna SR2 → 2026-03) into tagged clover-eclipse-libs pom+JAR installs for compatibility matrix
- [Eclipse Functional Testing Plan](eclipse-functional-testing.md) — Implementation plan for org.openclover.eclipse.functest + functest.runner: module layout, download pipeline, IApplication runner, test project triage, CI workflow
- [clover-idea build migrated to Gradle](clover-idea-libs.md) — clover-idea-libs deleted; clover-idea now built/tested/packaged by Gradle (IntelliJ Platform Gradle Plugin); pom.xml is a thin wrapper
- [RenderFileAction NPE — static ThreadLocals](render-file-action-static-threadlocals.md) — issue #122: static ThreadLocals nulled by HtmlReporter's finally block break concurrent/failing HTML reports; still open in 5.0.0; not a Jenkins plugin bug
