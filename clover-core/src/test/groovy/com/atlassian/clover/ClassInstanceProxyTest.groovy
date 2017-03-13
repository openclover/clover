package com.atlassian.clover

import junit.framework.TestCase

import java.lang.reflect.Field

import com.atlassian.clover.util.PrecannedClassLoader

class ClassInstanceProxyTest extends TestCase {
    void testIdentiy() {
        final ClassInstanceProxy object1 = new ClassInstanceProxy(Object.class)
        assertEquals(object1, object1)
    }

    void testNull() {
        final ClassInstanceProxy object1 = new ClassInstanceProxy(Object.class)
        assertFalse(object1.equals(null))
    }

    void testDuplicates() {
        final ClassInstanceProxy object1 = new ClassInstanceProxy(Object.class)
        final ClassInstanceProxy object2 = new ClassInstanceProxy(Object.class)
        assertEquals(object1.hashCode(), object2.hashCode())
        assertEquals(object1, object2)
    }

    void testDifferent() {
        final ClassInstanceProxy object = new ClassInstanceProxy(Object.class)
        final ClassInstanceProxy string = new ClassInstanceProxy(String.class)
        assertFalse(object.equals(string))
    }

    void testDifferentClassloaders() throws Exception {
        final InputStream stream = getClass().getResourceAsStream("/" + ClassInstanceProxy.class.getName().replace('.', '/') + ".class")
        final ByteArrayOutputStream baos = new ByteArrayOutputStream()
        final byte[] buffer = new byte[1000]

        int read = 0
        while (read != -1) {
            baos.write(buffer, 0, read)
            read = stream.read(buffer)
        }

        PrecannedClassLoader otherClassLoader =
            new PrecannedClassLoader(
                getClass().getClassLoader(),
                new HashMap() {{put(ClassInstanceProxy.class.getName(), baos.toByteArray());}})

        final ClassInstanceProxy self1 = new ClassInstanceProxy(ClassInstanceProxy.class)
        final ClassInstanceProxy self2 = new ClassInstanceProxy(
                otherClassLoader.loadClass(ClassInstanceProxy.class.getName(), true))
        assertFalse(self1.equals(self2))
        assertFalse(self1.hashCode() == self2.hashCode())
    }

    void testClassAndClassloaderHashCollisionForClassesWithDifferentNames() throws Exception {
        final ClassInstanceProxy object = new ClassInstanceProxy(Object.class)
        final ClassInstanceProxy string = new ClassInstanceProxy(String.class)
        final Field hashCodeField = ClassInstanceProxy.class.getDeclaredField("hashCode")
        hashCodeField.setAccessible(true)
        
        hashCodeField.set(string, new Integer(Object.class.hashCode()))
        assertFalse(object.equals(string))
    }
}
