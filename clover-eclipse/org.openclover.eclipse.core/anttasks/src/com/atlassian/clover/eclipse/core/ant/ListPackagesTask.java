package com.atlassian.clover.eclipse.core.ant;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.Enumeration;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Iterator;

import sun.tools.jar.resources.jar;

/**
 * An tasks to provide the ordered set of package names present in a jar.
 */
public class ListPackagesTask extends Task {

    private File file;
    private String propertyName;
    private String separator = ",";

    public void setJar(File file) {
        this.file = file;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public void execute() {
        if (propertyName == null) {
            throw new BuildException("propertyName property not set");
        }
        if (file == null) {
            throw new BuildException("file property not set");
        }
        try {
            JarFile jarFile = new JarFile(file);

            SortedSet<String> packages = new TreeSet<String>();

            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName.endsWith(".class")) {
                    String packageName =
                        entryName.substring(0, Math.max(0, entryName.lastIndexOf('/'))).replace('/', '.');
                    if (packageName.length() > 0) {
                        packages.add(packageName);
                    }
                }
            }

            StringBuffer buffer = new StringBuffer(1000);
            for (Iterator<String> iterator = packages.iterator(); iterator.hasNext();) {
                buffer.append(iterator.next());
                if (iterator.hasNext()) {
                    buffer.append(separator);
                }
            }

            getProject().setProperty(propertyName, buffer.toString());
        } catch (IOException e) {
            throw new BuildException("Unable to read jar", e);
        }
    }

}
