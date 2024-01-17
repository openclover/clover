package com.atlassian.clover.cfg.instr.java;

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
    SWITCH_EXPRESSIONS
}
