package com.atlassian.clover.util

import groovy.transform.CompileStatic

@CompileStatic
class PrecannedClassLoader extends ClassLoader {
    private Map<String, byte[]> precanned = new HashMap<String, byte[]>()

    PrecannedClassLoader(ClassLoader classLoader, Map<String, byte[]> precanned) {
        super(classLoader)
        this.precanned.putAll(precanned)
    }

    @Override
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

    @Override
    protected Class findClass(String name) throws ClassNotFoundException {
        if (precanned.containsKey(name)) {
            byte[] bytes = (byte[]) precanned.get(name)
            return defineClass(name, bytes, 0, bytes.length)
        } else {
            return super.findClass(name)
        }
    }
}
