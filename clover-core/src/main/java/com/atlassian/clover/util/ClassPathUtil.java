package com.atlassian.clover.util;

import com.atlassian.clover.api.CloverException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;
import java.security.CodeSource;

/**
 * A Util class used for manipulating and querying the System Class path.
 */
public class ClassPathUtil {

    private static void addURL(@NotNull URLClassLoader ucl, @NotNull URL path)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, CloverException {
        try {
            Method addURLMethod = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
            addURLMethod.setAccessible(true);
            addURLMethod.invoke(ucl, new Object[]{path});
        } catch (NoSuchMethodException e) {
            // should not happen
            throw new CloverException("Unable to setup classpath extender", e);
        }
    }

    /**
     * extends the system classpath
     * @param path the path to add
     * @param loader the class loader to extend
     * @throws CloverException if there is a problem extending the class path
     */
    public static void extendClassPath(@NotNull String path, @NotNull URLClassLoader loader) throws CloverException {

        try {
            URL url = new File(path).toURI().toURL();
            addURL(loader, url);
        } catch (Exception e) {
            throw new CloverException("Unable to add paths to the ClassLoader", e);
        }
    }

    /**
     * Returns the first URLClassLoader found in the class loader ancestry.
     * @param obj an Object to find the system class loader for
     * @return first URLClassLoader found in the class loader ancestry
     * @throws CloverException if no URLClassLoader can be found
     */
    @NotNull
    public static URLClassLoader findSystemClassLoader(@NotNull Class obj) throws CloverException {
        ClassLoader loader = obj.getClassLoader();
        while (!(loader instanceof URLClassLoader)) {            
            loader = loader.getParent();
            if (loader == null) {
                throw new CloverException("Unable to find a URLClassLoader for object: " + obj);
            }
        }
        return (URLClassLoader) loader;
    }

    /**
     * Determines whether the given className can be loaded from
     * the System Class Loader.
     * @see #findSystemClassLoader(Class) 
     * @param className the name of the class to test
     * @param loader the class loader to check
     * @return <code>true</code> if the class is on the system classpath, <code>false</code> otherwise
     */
    public static boolean isClassOnClassPath(@NotNull String className, @NotNull ClassLoader loader) {
        try {
            Class.forName(className, true, loader);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void assertOnClassPath(@NotNull String className, @NotNull ClassLoader loader) throws CloverException {
        if (!isClassOnClassPath(className, loader)) {
            throw new CloverException(className + " is not on classpath of class loader: " + loader);
        }
    }

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
