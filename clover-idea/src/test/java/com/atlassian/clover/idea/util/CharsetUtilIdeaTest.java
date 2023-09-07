package com.atlassian.clover.idea.util;

import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.intellij.openapi.vfs.encoding.EncodingProjectManager;
import com.intellij.testFramework.LightIdeaTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeThat;

/**
 * Encodings we have:
 *  - system encoding - usually OS-default one, can be set for JVM via -Dfile.encoding
 *  - ide encoding - used by IDEA when no project is openend, e.g. during source checkout
 *  - project encoding - used by IDEA as default encoding for all files in a project
 *  - file encoding - you can declare encoding on a file-by-file basis
 */
public class CharsetUtilIdeaTest extends LightIdeaTestCase {
    final String defaultSystemCharset = getName(CharsetToolkit.getDefaultSystemCharset());
    String ideCharset;
    String projectCharset;

    @Nullable
    private static String getName(@Nullable Charset charset) {
        return charset != null ? charset.name() : null;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ideCharset = getName(EncodingManager.getInstance().getDefaultCharset());
        projectCharset = getName(EncodingProjectManager.getInstance().getDefaultCharset());
    }

    @Override
    protected void tearDown() throws Exception {
        setIDEEncoding(ideCharset);
        setProjectEncoding(projectCharset);
        super.tearDown();
    }

    public void testSystemEncodingEqualsToFileEncoding() {
        // OS-default one
        assertThat(CharsetUtil.getSystemDefaultEncoding(), equalTo(System.getProperty("file.encoding")));
    }

    public void testEmptyIdeEncodingFallsBackToSystemEncoding() throws Exception {
        setIDEEncoding("");
        assertThat(CharsetUtil.getIdeDefaultEncoding(), equalTo(CharsetUtil.getSystemDefaultEncoding()));
    }

    public void testIdeEncodingCanOverrideSystemEncoding() throws Exception {
        // we need a different system encoding
        if ("UTF-16".equals(CharsetUtil.getSystemDefaultEncoding())) {
            return;
        }

        setIDEEncoding("UTF-16");
        assertThat(CharsetUtil.getIdeDefaultEncoding(), equalTo("UTF-16"));
    }

    public void testEmptyProjectEncodingFallsBackToSystemEncoding() throws Exception {
        setProjectEncoding("");
        assertThat(CharsetUtil.getProjectDefaultEncoding(), equalTo(CharsetUtil.getSystemDefaultEncoding()));
    }

    public void testProjectEncodingCanOverrideSystemEncoding() throws Exception {
        // we need a different system encoding
        if ("UTF-16".equals(CharsetUtil.getSystemDefaultEncoding())) {
            return;
        }

        setProjectEncoding("UTF-16");
        assertThat(CharsetUtil.getProjectDefaultEncoding(), equalTo("UTF-16"));
    }

    public void testGetFileEncodingFallsBackToProjectAndNotIdeEncoding() throws Exception {
        // in case when file encoding for a single file is not set, it shall fallback to project encoding
        // (and NOT IDE encoding as we don't run Clover without a project)

        // we need a different system encoding
        if ("UTF-16".equals(CharsetUtil.getSystemDefaultEncoding())
                || "ISO-8859-9".equals(CharsetUtil.getSystemDefaultEncoding())) {
            return;
        }

        // IDE and project charsets are different
        setIDEEncoding("UTF-16");
        setProjectEncoding("ISO-8859-9");

        // create file with no encoding set for it
        VirtualFile vf1 = createFile("File1.java", "").getVirtualFile();
        assertNotNull(vf1);

        // we shall use project setting
        assertThat(CharsetUtil.getFileEncoding(vf1), equalTo("ISO-8859-9"));
    }

    public void testGetFileEncodingFallsBackToSystemIfProjectEncodingIsNotSet() throws Exception {
        // we need a different system encoding
        if ("UTF-16".equals(CharsetUtil.getSystemDefaultEncoding())) {
            return;
        }

        // project encoding is not set
        setProjectEncoding("");

        VirtualFile vf1 = createFile("File1.java", "").getVirtualFile();
        VirtualFile vf2 = createFile("File2.java", "").getVirtualFile();
        VirtualFile vf3 = createFile("File3.java", "").getVirtualFile();
        assertNotNull(vf1);
        assertNotNull(vf2);
        assertNotNull(vf3);
        // vf1 encoding is null
        setFileEncoding(vf2, "UTF-16");
        setFileEncoding(vf3, defaultSystemCharset);

        assertThat(CharsetUtil.getFileEncoding(vf1), equalTo(defaultSystemCharset));
        assertThat(CharsetUtil.getFileEncoding(vf2), equalTo("UTF-16"));
        assertThat(CharsetUtil.getFileEncoding(vf3), equalTo(defaultSystemCharset));
    }

    public void testGetFileEncodingFallsProjectEncodingIsNotSet() throws Exception {
        // we need a different system encoding
        if ("ISO-8859-9".equals(CharsetUtil.getSystemDefaultEncoding())) {
            return;
        }

        // project encoding is set and is different from system one
        setProjectEncoding("ISO-8859-9");

        VirtualFile vf1 = createFile("File1.java", "").getVirtualFile();
        VirtualFile vf2 = createFile("File2.java", "").getVirtualFile();
        VirtualFile vf3 = createFile("File3.java", "").getVirtualFile();
        assertNotNull(vf1);
        assertNotNull(vf2);
        assertNotNull(vf3);
        // vf1 encoding is null
        setFileEncoding(vf2, "UTF-16");
        setFileEncoding(vf3, "ISO-8859-9");

        assertThat(CharsetUtil.getFileEncoding(vf1), equalTo("ISO-8859-9"));
        assertThat(CharsetUtil.getFileEncoding(vf2), equalTo("UTF-16"));
        assertThat(CharsetUtil.getFileEncoding(vf3), equalTo("ISO-8859-9"));
    }

    private void setIDEEncoding(@NotNull String encoding) throws Exception {
        EncodingManager.getInstance().setDefaultCharsetName(encoding);
    }

    private void setProjectEncoding(@NotNull String encoding) throws Exception {
        // store in project manager settings as well as ...
        EncodingProjectManager.getInstance().setDefaultCharsetName(encoding);
        // in project root - setEncoding(null, ...) is project root
        EncodingProjectManager.getInstance().setEncoding(null, !encoding.isEmpty() ? Charset.forName(encoding) : null);
    }

    private void setFileEncoding(VirtualFile file, String encoding) throws Exception {
        EncodingManager.getInstance().setEncoding(file, Charset.forName(encoding));
    }

}
