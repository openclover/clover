package org.openclover.core.registry.entities

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.api.registry.ProjectInfo
import org.openclover.core.registry.FixedSourceRegion

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull

class FullProjectInfoTest {

    @Rule
    public TestName testName = new TestName()

    @Test
    void testPackageLookup() {
        ProjectInfo project = new FullProjectInfo(testName.methodName)
        project.addPackage(new FullPackageInfo(project, "com.foo", 0))
        project.addPackage(new FullPackageInfo(project, "com.foo.bar", 1))
        project.addPackage(new FullPackageInfo(project, "com.foo.baz", 2))

        assertNotNull(project.findPackageFragment("com"))
        assertEquals("com", project.findPackageFragment("com").getName())
        assertNotNull(project.findPackageFragment("com.foo"))
        assertEquals("foo", project.findPackageFragment("com.foo").getName())
        assertNotNull(project.findPackageFragment("com.foo.bar"))
        assertEquals("bar", project.findPackageFragment("com.foo.bar").getName())
        assertNotNull(project.findPackageFragment("com.foo.baz"))
        assertEquals("baz", project.findPackageFragment("com.foo.baz").getName())
        assertNull(project.findPackageFragment("com.foo.faz"))
    }

    @Test
    void testPackageFileClassLookupAfterInvalidateCache() {
        ProjectInfo project = new FullProjectInfo(testName.methodName)

        //Force cache construction prematurely
        assertNull(project.findPackageFragment("com"))
        assertNull(project.findClass("com.foo.Foo"))
        assertNull(project.findFile("com/foo/Foo.java"))

        final PackageInfo pkg = new FullPackageInfo(project, "com.foo", 0)
        project.addPackage(pkg)

        final FullFileInfo file = new FullFileInfo(pkg, new File("Foo.java"), "UTF-8", 0, 0, 0, 0L, 0L, 0L, 0L)
        pkg.addFile(file)
        final FullClassInfo clazz = new FullClassInfo(pkg, file, 0, "Foo",
                new FixedSourceRegion(0, 0), new Modifiers(),
                false, false, false)
        file.addClass(clazz)

        //Not in cache, needs invalidating
        assertNull(project.findPackageFragment("com"))
        assertNull(project.findClass("com.foo.Foo"))
        assertNull(project.findFile("com/foo/Foo.java"))

        project.invalidateCaches()

        assertNotNull(project.findPackageFragment("com"))
        assertNotNull(project.findClass("com.foo.Foo"))
        assertNotNull(project.findFile("com/foo/Foo.java"))
    }
}
