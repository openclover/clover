package org.openclover.eclipse.core.views.dashboard;

import clover.org.apache.velocity.exception.ResourceNotFoundException;
import clover.org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.InputStream;
import java.util.Set;
import java.util.zip.InflaterInputStream;

import static org.openclover.core.util.Sets.newHashSet;

public class PlainTextVelocityResourceLoader extends ClasspathResourceLoader {
    private static final Set<String> PLAIN_TEXT = newHashSet();
    
    public static void addPlainTextResource(String resource) {
        PLAIN_TEXT.add(resource);
    }
    
    @Override
    public synchronized InputStream getResourceStream(String aName)
            throws ResourceNotFoundException {
        InputStream in = super.getResourceStream(aName);
        if (in == null) {
            throw new ResourceNotFoundException("Resource not found: " + aName);
        }
        return PLAIN_TEXT.contains(aName) ? in : new InflaterInputStream(in);
    }

}
