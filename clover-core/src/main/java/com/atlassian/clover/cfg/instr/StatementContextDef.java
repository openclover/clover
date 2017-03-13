package com.atlassian.clover.cfg.instr;

import com.atlassian.clover.api.CloverException;

import java.io.Serializable;

/**
*
*/
public class StatementContextDef implements Serializable {
    private String name;
    private String regexp;


    public StatementContextDef() {
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
}
