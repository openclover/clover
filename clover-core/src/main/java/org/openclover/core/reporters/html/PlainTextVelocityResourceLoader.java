package org.openclover.core.reporters.html;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.util.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Set;
import java.util.zip.InflaterInputStream;

import static org.openclover.core.util.Sets.newHashSet;

/**
 * A classpath resource loader which transparently inflates templates, except for the ones
 * explicitly registered as plain text via {@link #addPlainTextResource(String)}.
 */
public class PlainTextVelocityResourceLoader extends ClasspathResourceLoader {
    private static final Set<String> PLAIN_TEXT = newHashSet();

    public static void addPlainTextResource(String resource) {
        PLAIN_TEXT.add(resource);
    }

    @Override
    public synchronized Reader getResourceReader(String aName, String encoding)
            throws ResourceNotFoundException {
        final InputStream in = ClassUtils.getResourceAsStream(getClass(), aName);
        if (in == null) {
            throw new ResourceNotFoundException("Resource not found: " + aName);
        }
        try {
            return buildReader(PLAIN_TEXT.contains(aName) ? in : new InflaterInputStream(in), encoding);
        } catch (IOException ex) {
            throw new ResourceNotFoundException("Could not read resource: " + aName, ex);
        }
    }

}
