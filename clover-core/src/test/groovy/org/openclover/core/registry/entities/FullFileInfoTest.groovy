package org.openclover.core.registry.entities

import org.openclover.core.spi.lang.Language
import org.junit.Test

import static org.junit.Assert.assertNull
import static org.junit.Assert.assertSame

class FullFileInfoTest {

    @Test
    void testLanguage() {
        assertSame(Language.Builtin.JAVA,
                new FullFileInfo(null, new File("Foo.java"), "UTF-8", 0, 0, 0, 0, 0, 0, 0).getLanguage())
        assertSame(Language.Builtin.GROOVY,
                new FullFileInfo(null, new File("Foo.groovy"), "UTF-8", 0, 0, 0, 0, 0, 0, 0).getLanguage())
        assertNull(new FullFileInfo(null,
                new File("Foo.jsp"), "UTF-8", 0, 0, 0, 0, 0, 0, 0).getLanguage())
    }
}
