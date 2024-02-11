package org.openclover.core.registry.entities

import com.atlassian.clover.registry.FixedSourceRegion
import com.atlassian.clover.registry.entities.FullClassInfo
import com.atlassian.clover.registry.entities.FullFileInfo
import com.atlassian.clover.registry.entities.FullPackageInfo
import com.atlassian.clover.registry.entities.FullProjectInfo
import com.atlassian.clover.registry.entities.Modifiers
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

import static org.junit.Assert.*

class FullPackageInfoTest {

    @Rule
    public TestName testName = new TestName()

    @Test
    void testFileLookupAfterCacheInvalidated() {
        FullProjectInfo project = new FullProjectInfo(testName.methodName)

        final FullPackageInfo pkg = new FullPackageInfo(project, "com.foo", 0)
        project.addPackage(pkg)

        assertNull(pkg.getFileInPackage("Foo.java"))
        assertEquals(0, pkg.getClasses().size())

        final FullFileInfo file = new FullFileInfo(pkg, new File("Foo.java"), "UTF-8", 0, 0, 0, 0L, 0L, 0L, 0L)
        pkg.addFile(file)
        final FullClassInfo clazz = new FullClassInfo(pkg, file, 0, "Foo",
                new FixedSourceRegion(0, 0), new Modifiers(),
                false, false, false)
        file.addClass(clazz)

        assertEquals(0, pkg.getClasses().size())

        pkg.invalidateCaches()

        assertEquals(1, pkg.getClasses().size())
        assertSame(clazz, pkg.getClasses().get(0))
    }
}
