package org.openclover.core.registry.entities

import com.atlassian.clover.registry.entities.MethodSignature
import com.atlassian.clover.registry.entities.Modifiers
import com.atlassian.clover.registry.entities.Parameter
import org.junit.Test

import java.lang.reflect.Modifier

import static org.junit.Assert.assertEquals

class MethodSignatureTest {
    
    @Test
    void testGetNormalizedSignature() throws Exception {
        MethodSignature sig = new MethodSignature("name", "<T extends Object>", "String", null, null, Modifiers.createFrom(Modifier.ABSTRACT, null))
        assertEquals("abstract <T extends Object> String name()", sig.normalizedSignature)

        sig = new MethodSignature("name", null, "String", null, null, Modifiers.createFrom(Modifier.ABSTRACT, null))
        assertEquals("abstract String name()", sig.normalizedSignature)

        Parameter[] params = [new Parameter("int", "count"), new Parameter("String", "name") ]
        String[] throwsTypes = [ "Exception1", "Exception2" ]
        sig = new MethodSignature("name", null, "String", params, throwsTypes, Modifiers.createFrom(Modifier.ABSTRACT | Modifier.PUBLIC, null))
        assertEquals("public abstract String name(int count, String name) throws Exception1, Exception2", sig.normalizedSignature)
    }

    @Test
    void testGetVisibility() throws Exception {
        MethodSignature sig = new MethodSignature("name", null, "String", null, null, Modifiers.createFrom(Modifier.PRIVATE, null))
        assertEquals("private", sig.modifiers.visibility)
        sig = new MethodSignature("name", null, "String", null, null, Modifiers.createFrom(Modifier.PROTECTED, null))
        assertEquals("protected", sig.modifiers.visibility)
        sig = new MethodSignature("name", null, "String", null, null, Modifiers.createFrom(Modifier.PUBLIC, null))
        assertEquals("public", sig.modifiers.visibility)
        sig = new MethodSignature("name", null, "String", null, null, Modifiers.createFrom(0, null))
        assertEquals("package", sig.modifiers.visibility)
    }

}
