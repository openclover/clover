package com.atlassian.clover.idea.util;

import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.encoding.EncodingProjectManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;

public class CharsetUtil {
    private CharsetUtil() {
    }

    /**
     * Returns encoding set in the system (e.g. via the -Dfile.encoding JVM property)
     * @return String name of the charset
     */
    @Nullable
    public static String getSystemDefaultEncoding() {
        final Charset systemCharset = CharsetToolkit.getDefaultSystemCharset();
        return systemCharset != null ? systemCharset.name() : null;
    }

    /**
     * Returns default encoding for a project (as in Settings / Editor / File encodings / IDE encoding)
     * @return String name of the charset
     */
    @Nullable
    public static String getIdeDefaultEncoding() {
        final String systemCharsetName = getSystemDefaultEncoding();
        final Charset ideCharset = EncodingManager.getInstance().getDefaultCharset();
        return ideCharset != null ? ideCharset.name() : systemCharsetName;
    }

    /**
     * Returns default encoding for a project (as in Settings / Editor / File encodings / Project encoding)
     * @return String name of the charset
     */
    @Nullable
    public static String getProjectDefaultEncoding() {
        final String systemCharsetName = getSystemDefaultEncoding();
        final Charset projectCharset = EncodingProjectManager.getInstance().getDefaultCharset();
        return projectCharset != null ? projectCharset.name() : systemCharsetName;
    }

     /**
     * Returns encoding for given file. If not specified explicitly (as in Settings / Editor / File encodings / Override
     * encodings for files) it will return default project encoding.
     * @param virtualFile file
     * @return String name of the charset
     */
    public static String getFileEncoding(@NotNull VirtualFile virtualFile) {
        final Charset fileCharset = EncodingManager.getInstance().getEncoding(virtualFile, true);
        final String projectCharsetName = getProjectDefaultEncoding();
        return fileCharset != null
                ? fileCharset.name()
                : projectCharsetName;
    }

}
