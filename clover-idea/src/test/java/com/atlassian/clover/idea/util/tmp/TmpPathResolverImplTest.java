package com.atlassian.clover.idea.util.tmp;

import com.atlassian.clover.idea.util.tmp.TmpPathResolver;
import com.atlassian.clover.idea.util.tmp.TmpPathResolverImpl;
import com.atlassian.clover.idea.util.vfs.DummyVirtualFile;
import com.intellij.openapi.vfs.VirtualFile;
import junit.framework.TestCase;

public class TmpPathResolverImplTest extends TestCase {
    public void testWeakness() {
        TmpPathResolver resolver = new TmpPathResolverImpl();

        assertNull(resolver.getMapping("somePath"));
        assertNull(resolver.getMapping("differentPath"));

        VirtualFile vf1 = new DummyVirtualFile("name");
        @SuppressWarnings({"RedundantStringConstructorCall"})
        String path1 = new String("somePath");

        resolver.registerMapping(vf1, path1);
        assertSame(vf1, resolver.getMapping("somePath"));

        resolver.registerMapping(vf1, "differentPath");
        assertNull(resolver.getMapping("somePath"));

    }
}
