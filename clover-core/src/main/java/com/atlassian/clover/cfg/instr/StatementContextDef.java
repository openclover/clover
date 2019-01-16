package com.atlassian.clover.cfg.instr;

import com.atlassian.clover.api.CloverException;

import java.io.Serializable;


public class StatementContextDef implements Serializable {
    private String name;
    private String regexp;


    public StatementContextDef() {
    }

    public StatementContextDef(String name, String regexp) {
        this.name = name;
        this.regexp = regexp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegexp() {
        return regexp;
    }

    public void setRegexp(String regexp) {
        this.regexp = regexp;
    }

    public void validate() throws CloverException {
        if (name == null || name.trim().length() == 0) {
            throw new CloverException("Context definition requires a name");
        }

        if (regexp == null || regexp.trim().length() == 0) {
            throw new CloverException("Context definition requires a regular expression");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StatementContextDef that = (StatementContextDef) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return regexp != null ? regexp.equals(that.regexp) : that.regexp == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (regexp != null ? regexp.hashCode() : 0);
        return result;
    }
}
