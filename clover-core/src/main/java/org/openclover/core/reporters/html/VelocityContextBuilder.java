package org.openclover.core.reporters.html;

import org.apache.velocity.VelocityContext;

public class VelocityContextBuilder {

    private final VelocityContext context = new VelocityContext();

    private VelocityContextBuilder() {
    }

    public static VelocityContextBuilder create() {
        return new VelocityContextBuilder();
    }

    public VelocityContextBuilder put(String key, Object value) {
        context.put(key, value);
        return this;
    }

    public VelocityContext build() {
        return context;
    }

    public Object get(String key) {
        return context.get(key);
    }
}
