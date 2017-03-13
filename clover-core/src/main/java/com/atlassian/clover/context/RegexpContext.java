package com.atlassian.clover.context;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public abstract class RegexpContext extends SimpleContext {
    private Pattern pattern;

    protected RegexpContext(int index, String name, Pattern pattern) {
        super(index, name);
        this.pattern = pattern;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public boolean matches(String matchString) {
        Matcher m = pattern.matcher(matchString);
        return m.matches();
    }

    public boolean isEquivalent(RegexpContext other) {
        return other.getPattern().pattern().equals(pattern.pattern());
    }

    /**
     * It's a code context based on regular expressions.
     */
    @Override
    public Type getType() {
        return Type.REGEXP;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RegexpContext that = (RegexpContext) o;

        if (pattern != null ? !pattern.pattern().equals(that.pattern.pattern()) : that.pattern != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (pattern != null ? pattern.pattern().hashCode() : 0);
        return result;
    }

    public String toString() {
        return getName() + ":" + getIndex() + ":" + getPattern().pattern();
    }

}
