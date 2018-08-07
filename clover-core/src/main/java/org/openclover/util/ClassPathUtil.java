package org.openclover.util;

import com.atlassian.clover.util.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.io.File;
import java.security.CodeSource;

/**
 * Manipulating and querying the class path.
 */
public class ClassPathUtil {

    /**
     * Returns full path to a Clover JAR file or <code>null</code> if it was unable to determine.
     * @return String path or null
     */
    @Nullable
    public static String getCloverJarPath() {
        // first approach: try to locate the class as the resource using class loader
        String pathToClass = "/"+ ClassPathUtil.class.getName().replace('.','/') + ".class";
        URL url = ClassPathUtil.class.getResource(pathToClass);

        if (url != null) {
            String path = url.toString();
            try {
                String uri = null;
                // jar:file:!/path/to/clover.jar/com/atlassian/clover/util/ClassPathUtil.class
                if (path.startsWith("jar:file:")) {
                    int bang = path.indexOf("!");
                    uri = path.substring(4, bang);
                }
                // file:/path/to/classes/directory/com/atlassian/clover/util/ClassPathUtil.class
                else if (path.startsWith("file:")) {
                    int tail = path.indexOf(pathToClass);
                    uri = path.substring(0, tail);
                }
                if (uri != null) {
                    return (new File(FileUtils.fromURI(uri))).getAbsolutePath();
                }
            } catch (Exception e) {
                return null;
            }
        }

        // second approach: try to locate source using protection domain (case for clover.jar packed as OSGI bundle)
        try
        {
            CodeSource code = ClassPathUtil.class.getProtectionDomain().getCodeSource();
            if (code != null) {
                URI uri = code.getLocation().toURI();
                // reference:file:/path/to/clover.jar
                if ("reference".equals(uri.getScheme())) {
                    return (new File(FileUtils.fromURI(uri.getSchemeSpecificPart()))).getAbsolutePath();
                }
                // file:/path/to/classes/directory
                else if ("file".equals(uri.getScheme())) {
                    return (new File(FileUtils.fromURI(uri.toString()))).getAbsolutePath();
                }
            }
        } catch (SecurityException ex) {
            return null;
        } catch (URISyntaxException ex) {
            return null;
        }

        // no success
        return null;
    }

}
