package com.atlassian.clover.context;

public class SimpleContext implements NamedContext {
    private int index;
    private String name;

    SimpleContext(int index, String name) {
        this.index = index;
        this.name = name;
    }

    @Override
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns information that it's s a built-in block code context.
     */
    @Override
    public Type getType() {
        return Type.BLOCK;
    }

    public String toString() {
        return getName() + ":" + getIndex();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleContext that = (SimpleContext) o;

        if (index != that.index) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = index;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
