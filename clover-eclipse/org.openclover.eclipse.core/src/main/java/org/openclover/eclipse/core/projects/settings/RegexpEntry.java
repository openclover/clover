package org.openclover.eclipse.core.projects.settings;

import java.util.StringTokenizer;

public class RegexpEntry {

    public static final int METHOD_TYPE = 0;
    public static final int STATEMENT_TYPE = 1;

    private String name;
    private String regexp;
    private int type;
    private boolean changed;
    private final String originalName;

    public RegexpEntry(String str) {
        StringTokenizer tokens = new StringTokenizer(str, ",", true);

        //No longer peristed property 'enabled'
        Boolean.valueOf(tokens.nextToken()).booleanValue();
        tokens.nextToken(); // the delimiter token
        type = (Integer.parseInt(tokens.nextToken()));
        tokens.nextToken(); // the delimiter token
        //No longer used property 'index'
        Integer.parseInt(tokens.nextToken());
        tokens.nextToken(); // the delimiter token
        //No longer persisted property 'changed'
        Boolean.valueOf(tokens.nextToken()).booleanValue();
        changed = false;
        tokens.nextToken(); // the delimiter token
        originalName = name = (tokens.nextToken());
        tokens.nextToken(); // the delimiter token
        String s = "";
        while (tokens.hasMoreTokens()) {
            s += tokens.nextToken();
        }
        regexp = (s);
    }

    private RegexpEntry(String name, RegexpEntry original) {
        this.name = this.originalName = name;
        this.regexp = original.regexp;
        this.type = original.type;
        this.changed = true;
    }

    public RegexpEntry(String name, String regexp, int type) {
        this.name = this.originalName = name;
        this.regexp = regexp;
        this.type = type;
        this.changed = true;
    }

    public String getName() {
        return name;
    }

    public String getOriginalName() {
        return originalName;
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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String toString() {
        //enabled - no longer persisted
        String s = "" + true;
        s += "," + getType();
        //index - no longer persisted
        s += "," + -1;
        //changed - no longer persisted
        s += "," + false;
        s += "," + getName();
        s += "," + getRegexp();
        return s;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public boolean equivalent(RegexpEntry other) {
        return
            other == this
            || (other.name.equals(name)
                && other.regexp.equals(regexp)
                && other.type == type);
    }

    public RegexpEntry duplicate(String name) {
        return new RegexpEntry(name, this);
    }
}