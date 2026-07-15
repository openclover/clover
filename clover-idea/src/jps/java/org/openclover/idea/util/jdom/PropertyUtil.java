package org.openclover.idea.util.jdom;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;

import static org.openclover.core.util.Maps.newHashMap;

public class PropertyUtil {

    public static final Object getPropertyValue(String propertyName, Object obj) {
        // look for a method with a name like 'isPropertyName' or 'getPropertyName'
        String methodName = "is" + propertyName.substring(0, 1).toUpperCase(Locale.ENGLISH) + propertyName.substring(1);
        Class[] paramTypes = new Class[0];
        Method getter = null;
        try {
            getter = obj.getClass().getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            try {
                methodName = "get" + propertyName.substring(0, 1).toUpperCase(Locale.ENGLISH) + propertyName.substring(1);
                getter = obj.getClass().getMethod(methodName, paramTypes);
            } catch (NoSuchMethodException e1) {
            }
        }
        if (getter == null) {
            // no such property.
        }
        try {
            return getter.invoke(obj, new Object[0]);
        } catch (IllegalAccessException | InvocationTargetException e) {
        }
        return null;
    }

    public static Property[] getProperties(Class cls) {
        final Map<String, Property> properties = newHashMap();

        // analyse the methods defined on the class.
        final Method[] methods = cls.getMethods();
        for (Method method : methods) {
            if (isAccessor(method)) {
                Property property = getProperty(method);
                String key = property.getName() + ":" + property.getType().getName();
                if (!properties.containsKey(key)) {
                    properties.put(key, property);
                } else {
                    Property otherProperty = properties.get(key);
                    otherProperty.merge(property);
                }
            }
        }
        return properties.values().toArray(new Property[properties.size()]);
    }

    public static Property[] getProperties(Object obj) {
        if (obj == null) {
            return new Property[0];
        }
        return getProperties(obj.getClass());
    }

    private static boolean isAccessor(Method m) {
        String methodName = m.getName();
        // check the method name against the known accessor patterns.
        // a) getXxx
        // b) setXxx
        // c) isXxx
        String propName = null;
        if (methodName.startsWith("get")) {
            propName = methodName.substring(3);
        } else if (methodName.startsWith("set")) {
            propName = methodName.substring(3);
        } else if (methodName.startsWith("is")) {
            propName = methodName.substring(2);
        } else {
            return false;
        }
        if (propName.length() == 0) {
            return false;
        }

        // check that the arguments are correct.
        // a) setter has one arg, and no return type
        // b) getter has no arg, and a return type
        if (methodName.startsWith("get") || methodName.startsWith("is")) {
            // potential getter.
            if (m.getReturnType() == null) {
                return false;
            }
            if (m.getParameterTypes().length > 0) {
                return false;
            }
        } else {
            // potential setter.
            if (m.getReturnType() != Void.TYPE) {
                return false;
            }
            if (m.getParameterTypes().length != 1) {
                return false;
            }
        }

        return (Character.isUpperCase(propName.charAt(0)));
    }

    private static Property getProperty(Method m) {
        String methodName = m.getName();

        // strip the front of the method name.
        String propName = null;
        Class type = null;
        boolean isGetter = false;
        if (methodName.startsWith("get")) {
            propName = methodName.substring(3);
            type = m.getReturnType();
            isGetter = true;
        } else if (methodName.startsWith("set")) {
            propName = methodName.substring(3);
            type = m.getParameterTypes()[0];
        } else if (methodName.startsWith("is")) {
            propName = methodName.substring(2);
            type = m.getReturnType();
            isGetter = true;
        }

        if (propName.length() == 1) {
            propName = propName.toLowerCase();
        } else {
            propName = propName.substring(0, 1).toLowerCase() + propName.substring(1);
        }

        Property property = new Property(propName, type);
        if (isGetter) {
            property.setGetterMethod(m);
        } else {
            property.setSetterMethod(m);
        }
        return property;
    }
}


