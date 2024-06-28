package org.openclover.core.util;

import org.jetbrains.annotations.NotNull;
import org.openclover.runtime.util.IOStreamUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.StringTokenizer;


public class FileUtils {

    public static final String JAVA_IO_TMPDIR = "java.io.tmpdir";

    private final long PLATFORM_FS_RESOLUTION;
    private static final int DEFAULT_FS_RESOLUTION = 1000;
    private static FileUtils INSTANCE;

    private FileUtils(long res) {
        this.PLATFORM_FS_RESOLUTION = res;
    }

    public static synchronized FileUtils getInstance() {
        if (INSTANCE != null) {
            return INSTANCE;
        }

        INSTANCE = new FileUtils(calcFSResolution());
        return INSTANCE;
    }

    /**
     * Returns system's temporary directory as defined in java.io.tmpdir
     * @return File temporary directory
     * @throws java.lang.RuntimeException if java.io.tmpdir is not set
     */
    public static File getJavaTempDir() {
        try {
            final String property = System.getProperty(JAVA_IO_TMPDIR);
            if (property == null) {
                throw new RuntimeException("The " + JAVA_IO_TMPDIR + " system property is not set. " +
                        "Please ensure this property is set before executing OpenClover.");
            } else {
                return new File(property);
            }
        } catch (SecurityException ex) {
            throw new RuntimeException("The " + JAVA_IO_TMPDIR + " system property could not be read. " +
                    "Please ensure access to system properties is allowed before executing OpenClover.");
        }
    }

    public long getPlatformFSResolution() {
        return PLATFORM_FS_RESOLUTION;
    }

    private static int calcFSResolution() {
        int rez = 1;
        File temp;

        try {
            temp = File.createTempFile("clover_fs_rez_test", ".txt");

            try {
                //Limit resolution inquiry to 1s, just in case
                while (rez <= 1000) {
                    temp.setLastModified(rez);
                    if (temp.lastModified() == rez) {
                        break;
                    }
                    rez *= 10;
                }
                return rez;
            } finally {
                temp.delete();
            }
        } catch (Exception e) {
            //Assume 1s resolution in worst case (OSX & Win95)
            return DEFAULT_FS_RESOLUTION;
        }
    }
    
    public static void readerCopy(Reader src, Writer dest) throws IOException {
        BufferedReader in = null;
        PrintWriter out = null;
        try {
            in = new BufferedReader(src);
            out = new PrintWriter(dest);
            String line = in.readLine();
            while (line != null) {
                out.println(line);
                line = in.readLine();
            }
            out.flush();
        } finally {
            IOStreamUtils.close(in);
            IOStreamUtils.close(out);
        }
    }

    public static void fileCopy(File src, File dest)
            throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new BufferedInputStream(Files.newInputStream(src.toPath()));
            out = new BufferedOutputStream(Files.newOutputStream(dest.toPath()));
            int b = in.read();
            while (b >= 0) {
                out.write(b);
                b = in.read();
            }
        } finally {
            IOStreamUtils.close(in);
            IOStreamUtils.close(out);
        }
    }

    /**
     * Copies the whole content (files and subdirectories) from <pre>srcDir</pre> to <pre>destDir</pre> preserving
     * directory layout. If <pre>deleteDestDir</pre> is set to true, destination directory will be deleted if exists.
     * Otherwise files/subdirectories from source directory will be added to existing target directory (possibly
     * overwriting existing files). Note that timestamps are not preserved.
     */
    public static void dirCopy(File srcDir, File destDir, boolean deleteDestDir) throws IOException {
        // check input arguments
        if (!srcDir.isDirectory()) {
            throw new IOException("Source '" + srcDir.getAbsolutePath() + "' is not a directory");
        }
        if (srcDir.getAbsolutePath().equals(destDir.getAbsolutePath())) {
            throw new IOException("Target '" + destDir.getAbsolutePath()
                    + "' is same as source '" + srcDir.getAbsolutePath() + "'");
        }
        if (isAncestorOf(srcDir, destDir)) {
            throw new IOException("Target '" + destDir.getAbsolutePath()
                    + "' is sub-directory of source '" + srcDir.getAbsolutePath() + "'");
        }

        // optionally delete target dir (or file)
        if (deleteDestDir && destDir.exists()) {
            if (!deltree(destDir)) {
                throw new IOException("Unable to delete destination directory '" + destDir.getAbsolutePath() + "'");
            }
        }
        // create target dir if does not exist
        if (!destDir.exists() && !destDir.mkdir()) {
            throw new IOException("Failed to create destination directory '" + destDir.getAbsolutePath() + "'");
        }

        // copy files and folders
        File[] files = srcDir.listFiles();
        for (File file : files) {
            File destFile = new File(destDir, file.getName());
            if (file.isDirectory()) {
                dirCopy(file, destFile, deleteDestDir);
            } else {
                fileCopy(file, destFile);
            }
        }
    }

    /**
     * Read data from input stream <code>in</code> and write to <code>outFile</code> file.
     * @param in source
     * @param outFile target
     */
    public static void inputStreamToFile(final @NotNull InputStream in, final @NotNull File outFile) throws IOException {
        OutputStream out = null;
        outFile.getParentFile().mkdirs();
        try {
            out = new BufferedOutputStream(Files.newOutputStream(outFile.toPath()));
            streamCopy(in, out);
        } finally {
            IOStreamUtils.close(in);
            IOStreamUtils.close(out);
        }
    }

    public static void streamCopy(InputStream inputStream, OutputStream outputStream) throws IOException {
        final byte[] buffer = new byte[16 * 1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
    }

    /**
     * Copy <code>resourcePath</code> resource using <code>classLoader</code> to <code>outputFile</code>.
     */
    public static void resourceToFile(final ClassLoader classLoader, final String resourcePath, final File outputFile) throws IOException {
        // open the resource
        final InputStream res = classLoader.getResourceAsStream(resourcePath);
        if (res == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }
        // and save it to a file
        inputStreamToFile(res, outputFile);
    }

    /**
     * delete a file, or directory and all of its contents
     *
     * @param rootDir the file or directory to start at
     * @return success of the operation
     */
    public static boolean deltree(File rootDir) {
        if (rootDir.isDirectory()) {
            final String[] files = rootDir.list();
            for (String file : files) {
                if (!deltree(new File(rootDir, file))) {
                    return false;
                }
            }
        }
        return rootDir.delete();
    }

    /**
     * Converts all backslashes to forward-slashes, so that Unix-style file separator is used.
     * @return normalized path (or null if inputPath is null)
     */
    public static String getNormalizedPath(String inputPath) {
        return (inputPath == null ? null : inputPath.replace('\\', '/'));
    }

    /**
     * Converts all forward-slashes and back-slashes to a platform-specific file separator
     * (i.e. '/' on Unix, '\' on Windows). The File.separatorChar is used.
     * @return converted path (or null if inputPath is null)
     */
    public static String getPlatformSpecificPath(String inputPath) {
        return (inputPath == null ? null : inputPath.replace('\\', '/').replace('/', File.separatorChar));
    }

    public static String getRelativePath(String a, String b, String pathSep) {
        String[] pathA = getPaths(a);
        String[] pathB = getPaths(b);

        // get common root.
        int indexA = -1;
        int indexB = -1;

        for (int i = pathA.length; 0 < i; i--) {
            if (lastIndexOf(pathA[i - 1], pathB) != -1) {
                indexA = i - 1;
                indexB = lastIndexOf(pathA[i - 1], pathB);
                break;
            }
        }

        if (indexA == -1 || indexB == -1) {
            return null;
        }

        int pathADistance = pathA.length - (indexA + 1);
        int pathBDistance = pathB.length - (indexB + 1);

        String relativePath = "";
        for (int i = 0; i < pathADistance; i++) {
            relativePath += ".." + pathSep;
        }
//        if (pathADistance == 0) {
//            relativePath = "." + File.separator;
//        }

        String[] pathBComponents = getPathComponents(b);

        String sep = "";
        for (int i = pathB.length - pathBDistance; i < pathB.length; i++) {
            relativePath += sep + pathBComponents[i];
            sep = pathSep;
        }
        return relativePath;
    }

    public static String getRelativePath(String a, String b) {
        return getRelativePath(a, b, File.separator);
    }

    public static String getRelativePath(File a, File b, String pathSep) {
        return getRelativePath(a.getAbsolutePath(), b.getAbsolutePath(), pathSep);
    }

    public static String getRelativePath(File a, File b) {
        return getRelativePath(a.getAbsolutePath(), b.getAbsolutePath(), File.separator);
    }

    public static long calcAdlerChecksum(File f, String encoding) throws IOException {
        final char [] buffer = new char[8192];
        ChecksummingReader reader = null;
        try {
            final Reader in;
            if (encoding != null) {
                in = new InputStreamReader(Files.newInputStream(f.toPath()), encoding);
            } else {
                in = new FileReader(f);
            }
            reader = new ChecksummingReader(new BufferedReader(in));
            while (reader.read(buffer) >= 0);
            return reader.getChecksum();
        } finally {
            IOStreamUtils.close(reader);
        }
    }

    private static int lastIndexOf(String str, String[] path) {
        for (int i = path.length; 0 < i; i--) {
            if (path[i - 1].compareTo(str) == 0) {
                return i - 1;
            }
        }
        return -1;
    }

    private static String[] getPathComponents(String str) {
        StringTokenizer tokens = new StringTokenizer(str, "\\/", false);
        String[] result = new String[tokens.countTokens()];
        for (int i = 0; i < result.length; i++) {
            result[i] = tokens.nextToken();
        }
        return result;
    }

    private static String[] getPaths(String str) {
        StringTokenizer tokens = new StringTokenizer(str, "\\/", false);
        String[] result = new String[tokens.countTokens()];
        String currentPath = "";
        String pathSep = "";
        for (int i = 0; i < result.length; i++) {
            currentPath = currentPath + pathSep + tokens.nextToken();
            result[i] = currentPath;
            pathSep = File.separator;
        }
        return result;
    }

    public static boolean isAncestorOf(File a, File b) {
        if (b == null) {
            throw new IllegalArgumentException();
        }
        File ancestor = b.getParentFile();
        while (ancestor != null) {
            if (ancestor.equals(a)) {
                return true;
            }
            ancestor = ancestor.getParentFile();
        }
        return false;
    }

    /**
     * *** stolen from Apache Ant 1.6
     * Constructs a file path from a <code>file:</code> URI.
     *
     * <p>Will be an absolute path if the given URI is absolute.</p>
     *
     * <p>Swallows '%' that are not followed by two characters,
     * doesn't deal with non-ASCII characters.</p>
     *
     * @param uri the URI designating a file in the local filesystem.
     * @return the local file system path for the file.
     * @since Ant 1.6
     */
    public static String fromURI(String uri) {
        if (!uri.startsWith("file:")) {
            throw new IllegalArgumentException("Can only handle file: URIs");
        }
        if (uri.startsWith("file://")) {
            uri = uri.substring(7);
        } else {
            uri = uri.substring(5);
        }

        uri = uri.replace('/', File.separatorChar);
        if (File.pathSeparatorChar == ';' && uri.startsWith("\\") && uri.length() > 2
            && Character.isLetter(uri.charAt(1)) && uri.lastIndexOf(':') > -1) {
            uri = uri.substring(1);
        }

        StringBuilder sb = new StringBuilder();
        CharacterIterator iter = new StringCharacterIterator(uri);
        for (char c = iter.first(); c != CharacterIterator.DONE;
             c = iter.next()) {
            if (c == '%') {
                char c1 = iter.next();
                if (c1 != CharacterIterator.DONE) {
                    int i1 = Character.digit(c1, 16);
                    char c2 = iter.next();
                    if (c2 != CharacterIterator.DONE) {
                        int i2 = Character.digit(c2, 16);
                        sb.append((char) ((i1 << 4) + i2));
                    }
                }
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Compares a given date with the last modified date of a file, taking
     * into account the filesystem resolution for file dates available on the
     * current platform.
     *
     * @return 0 if the date and the last modified date match or are close enough
     * that they could match given the platform fs date resolution,
     * -1 if the file is newer than the date or 1 if the file is older.
     */
    public int compareLastModified(long date, File file) {
        return compareLastModified(date, file.lastModified());
    }

    /**
     * Compares a two dates, taking into account the filesystem resolution
     * for file dates available on the current platform.
     *
     * @return 0 if the dates match or are close enough that they could match
     * given the platform fs date resolution, -1 if the latter is newer than the former,
     * or 1 if the latter is older than the former
     */
    private int compareLastModified(long date1, long date2) {
        long diff = date1 - date2;

        if (Math.abs(diff) <= PLATFORM_FS_RESOLUTION / 2) {
            return 0;
        } else{
            return diff > 0 ? 1 : -1;
        }
    }

    /**
     * Lists only the files, not the directories for a given directory.
     * @param dir the dir to list files of
     * @param regex filenames must match this
     * @return an array of all files in the given dir
     * @throws java.io.IOException if the dir cannot be read
     */
    public static File[] listMatchingFilesForDir(File dir, final String regex) throws IOException {
        File [] files = dir.listFiles(file -> file.isFile() && file.getName().matches(regex));

        if (files == null) {
            throw new IOException("Unable to read directory " + dir);
        }
        return files;
    }

    public static File createTempDir(String name) throws IOException {
        return createTempDir(name, null);
    }

    public static File createTempDir(String name, File parent) throws IOException {

        final File temp;
        if (parent != null) {
            parent.mkdirs();
            temp = File.createTempFile(name, "", parent);
        } else {
            temp = File.createTempFile(name, "");
        }

        temp.delete();
        temp.mkdirs();
        return temp;
    }

    public static File createEmptyDir(File parent, String name) {
        final File emptyDir = new File(parent, name);
        deltree(emptyDir);
        emptyDir.mkdirs();
        return emptyDir;
    }

    public static String readChars(DataInputStream in) throws IOException {
        int len = in.readInt();
        StringBuilder buf = new StringBuilder(len);
        for(int i = 0; i < len; i++) {
            buf.append(in.readChar());
        }
        return buf.toString();
    }

    /**
     * @return new File(".") if dir is null
     */
    public static File getCurrentDirIfNull(File dir) {
        if (dir == null) {
            dir = new File(".");
        }
        return dir;
    }
}