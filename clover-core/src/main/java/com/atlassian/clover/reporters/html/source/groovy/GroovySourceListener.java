package com.atlassian.clover.reporters.html.source.groovy;

import com.atlassian.clover.reporters.html.source.java.JavaSourceListener;

public interface GroovySourceListener extends JavaSourceListener {
    void onRegexp(String s);
}
