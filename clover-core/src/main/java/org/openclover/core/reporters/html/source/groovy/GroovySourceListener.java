package org.openclover.core.reporters.html.source.groovy;

import org.openclover.core.reporters.html.source.java.JavaSourceListener;

public interface GroovySourceListener extends JavaSourceListener {
    void onRegexp(String s);
}
