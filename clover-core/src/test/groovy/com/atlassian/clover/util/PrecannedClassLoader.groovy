package com.atlassian.clover.util

import static org.openclover.util.Maps.newHashMap

class PrecannedClassLoader extends ClassLoader {
    private Map precanned = newHashMap()

    PrecannedClassLoader(ClassLoader classLoader, Map precanned) {
        super(classLoader)
        this.precanned.putAll(precanned)
    }

    Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class result = null
        try {
            result = findClassInternal(name)
            if (result != null && resolve) {
                resolveClass(result)
            }
        } catch (ClassNotFoundException e) {
        }
        if (result == null) {
            result = super.loadClass(name, resolve)
            if (result != null && resolve) {
                resolveClass(result)
            }
        }
        if (result == null) {
            throw new ClassNotFoundException(name)
        }
        return result
    }

    protected Class findClassInternal(String name) throws ClassNotFoundException {
        if (precanned.containsKey(name)) {
            byte[] bytes = (byte[]) precanned.get(name)
            return defineClass(name, bytes, 0, bytes.length)
        } else {
            return findClass(name)
        }
    }
}
