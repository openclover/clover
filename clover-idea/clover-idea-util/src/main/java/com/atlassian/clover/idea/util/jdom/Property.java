package com.atlassian.clover.idea.util.jdom;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Property {

    private final String name;
    private final Class type;

    private boolean readable;
    private boolean writeable;
    private Method getterMethod;
    private Method setterMethod;

    public Property(String propertyName, Class propertyType) {
        name = propertyName;
        type = propertyType;
    }

    public String getName() {
        return name;
    }

    public Class getType() {
        return type;
    }

    public Object getValue(Object obj) throws IllegalAccessException, InvocationTargetException {
        // varify that this property is being used correctly ??
        if (!isReadable()) {
            throw new IllegalArgumentException("Property " + name + " is not readable.");
        }
        getterMethod.setAccessible(true);
        return getterMethod.invoke(obj, new Object[0]);
    }

    public void setValue(Object obj, Object val) throws IllegalAccessException, InvocationTargetException {
        // varify that this property is being used correctly ??
        if (!isWriteable()) {
            throw new IllegalArgumentException("Property " + name + " is not writable.");
        }
        setterMethod.setAccessible(true);
        setterMethod.invoke(obj, new Object[]{val});
    }

    public boolean isReadable() {
        return readable;
    }

    public boolean isWriteable() {
        return writeable;
    }

    public void setSetterMethod(Method m) {
        setterMethod = m;
        writeable = true;
    }

    public void setGetterMethod(Method m) {
        getterMethod = m;
        readable = true;
    }

    public void merge(Property otherProperty) {
        if (!name.equals(otherProperty.name)) {
            throw new IllegalArgumentException("Can not merge properties with different names: (" + name + ", " + otherProperty.name + ").");
        }
        if (!type.equals(otherProperty.type)) {
            throw new IllegalArgumentException("Can not merge properties with different types: (" + type + ", " + otherProperty.type + ")");
        }
        if (otherProperty.getterMethod != null && !readable) {
            setGetterMethod(otherProperty.getterMethod);
        }
        if (otherProperty.setterMethod != null && !writeable) {
            setSetterMethod(otherProperty.setterMethod);
        }
    }
}
