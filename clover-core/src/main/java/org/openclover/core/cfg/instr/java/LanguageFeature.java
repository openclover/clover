package org.openclover.core.cfg.instr.java;

public enum LanguageFeature {
    /** Lambda functions and method references */
    LAMBDA,
    /** Module system, defined in module-info.java files */
    MODULES,
    /** Multi-line text blocks in """...""" */
    TEXT_BLOCKS,
    /** Record classes and compact canonical constructors */
    RECORDS,
    /** Switch treated as expressions */
    SWITCH_EXPRESSIONS,
    /** Pattern matching for switch and record deconstruction patterns (type patterns, guarded
     *  {@code when} clauses, {@code case null}, record patterns in switch and instanceof) */
    PATTERN_MATCHING,
    /** Flexible constructor bodies (JEP 513, Java 25): statements are allowed before an explicit
     *  {@code this(...)}/{@code super(...)} invocation. Gates where constructor entry
     *  instrumentation is anchored - before vs after the explicit invocation. */
    FLEXIBLE_CONSTRUCTORS
}
