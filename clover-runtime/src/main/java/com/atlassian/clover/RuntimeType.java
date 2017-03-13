package com.atlassian.clover;

import com_atlassian_clover.Clover;

public final class RuntimeType {
    public final String name;
    public final int id;

    public RuntimeType(String type) {
        this.name = type;
        this.id = Clover.getTypeID(type);
    }

    public boolean matches(String type) {
        return id == Clover.getTypeID(type) && name.equals(type);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RuntimeType that = (RuntimeType)o;

        if (id != that.id) return false;
        return name.equals(that.name);
    }

    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + id;
        return result;
    }

    public String toString() {
        return "RuntimeType[" + "name='" + name + '\'' + ", id=" + id + ']';
    }
}
