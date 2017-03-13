package com.atlassian.clover;

import com.atlassian.clover.cfg.Interval;
import com.atlassian.clover.registry.Clover2Registry;

public class CloverDatabaseSpec {

    private String initString;
    private Interval span = Interval.DEFAULT_SPAN;

    public CloverDatabaseSpec() {}

    public CloverDatabaseSpec(Clover2Registry reg) {
        this(reg.getRegistryFile().getAbsolutePath());
    }

    public CloverDatabaseSpec(String initString) {
        this(initString, Interval.DEFAULT_SPAN);
    }

    public CloverDatabaseSpec(String initString, Interval span) {
        this.initString = initString;
        this.span = span;
    }

    public void setInitString(String initString) {
        this.initString = initString;
    }

    public void setSpan(Interval span) {
        this.span = span;
    }

    public String getInitString() {
        return initString;
    }

    public Interval getSpan() {
        return span;
    }

}
