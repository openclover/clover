package com.atlassian.clover.util;

import com.atlassian.clover.Logger;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import static clover.com.google.common.collect.Lists.newLinkedList;

/**
 * Class holds chain of paths separated by a path separator (e.g "/my/path1:/my/path2")
 */
public class Path {
    /** Just a logger */
    private Logger log = Logger.getInstance();

    /**
     * A List<String> where each String is a single path
     */
    private List<String> elements = newLinkedList();

    /**
     * Constructs an empty path chain.
     */
    public Path() {

    }

    /**
     * Constructs a chain of paths from <pre>osPathString</pre> using a default platform path separator
     * (i.e. the File.pathSeparator value). All file separators within a path are being normalized to
     * have unix-style endings.
     * @see #Path(String, String)
     */
    public Path(String osPathString) {
        this(osPathString, File.pathSeparator);
    }

    /**
     * Constructs a chain of paths from <pre>osPathString</pre> using a specified <pre>pathSep</pre>
     * path separator. All file separators within a path are being normalized to have unix-style endings.
     * <p/>
     * Example:
     * <pre>
     *    new Path("/my/path1:\my\path2", ":")
     * will internally keep
     *      /my/path1
     *      /my/path2
     * </pre>
     */
    public Path(String osPathString, String pathSep) {
        for (StringTokenizer pt = new StringTokenizer(osPathString, pathSep); pt.hasMoreTokens();) {
            elements.add(FileUtils.getNormalizedPath(pt.nextToken()));
        }
    }

    /**
     * Constructs a chain of paths from <pre>elements</pre> array.
     * All file separators within a path are being normalized to have unix-style endings.
     */
    public Path(String [] elements) {
        addAll(Arrays.asList(elements));
        normalizeFileSeps();
    }

    public void append(String element) {
        elements.add(FileUtils.getNormalizedPath(element));
    }

    public void append(Path path) {
        elements.addAll(path.elements);
    }

    /**
     * @return a file on this path if one exists for the relativeFile, else returns null
     * @param relativeFile the relative file to search for. Always treated as a relative path: /foo.txt == foo.txt
     */
    public File resolveFile(String relativeFile) {
        relativeFile = FileUtils.getNormalizedPath(relativeFile);
        for (String pathElement : elements) {
            File absoluteFile = new File(pathElement, relativeFile);
            boolean exists = absoluteFile.exists();
            log.debug("looking for " + absoluteFile + (exists ? ": FOUND" : ": not found"));
            if (exists) {
                return absoluteFile;
            }
        }
        log.debug(relativeFile + " not found on path");
        return null;
    }

    protected void addAll(List<String> pathEls) {
        elements.addAll(pathEls);
    }


    protected void normalizeFileSeps() {
        List<String> newElements = newLinkedList();
        for (String element : elements) {
            newElements.add(FileUtils.getNormalizedPath(element));
        }
        elements = newElements;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        String sep = "";
        for (String pathElement : elements) {
            buf.append(sep);
            buf.append(pathElement);
            sep = File.pathSeparator;
        }
        return buf.toString();
    }
}
