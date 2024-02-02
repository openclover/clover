package org.openclover.idea.util.tmp;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.Map;
import java.util.WeakHashMap;

public class TmpPathResolverImpl implements TmpPathResolver {
    private Map<String, VirtualFile> mapping = new WeakHashMap<>();

    private static Key<String> KEY = Key.create(TmpPathResolverImpl.class.getName());

    @Override
    public void registerMapping(VirtualFile origFile, String tmpFile) {
        mapping.put(tmpFile, origFile);
        final String prev = origFile.getUserData(KEY);
        if (prev != null) {
            mapping.remove(prev);
        }
        origFile.putUserData(KEY, tmpFile);
    }

    @Override
    public VirtualFile getMapping(String tmpFile) {
        return mapping.get(tmpFile);
    }
}
