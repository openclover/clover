package com.atlassian.clover.util

import clover.com.google.common.collect.Maps

class PrecannedClassLoader extends ClassLoader {
    private Map precanned = Maps.newHashMap()

    PrecannedClassLoader(ClassLoader classLoader, Map precanned) {
        super(classLoader)
        this.precanned.putAll(precanned)
    }

    Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class result = null
        try {
            result = findClass(name)
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

    protected Class findClass(String name) throws ClassNotFoundException {
        if (precanned.containsKey(name)) {
            byte[] bytes = (byte[]) precanned.get(name)
            return defineClass(name, bytes, 0, bytes.length)
        } else {
            return super.findClass(name)
        }
    }
}
