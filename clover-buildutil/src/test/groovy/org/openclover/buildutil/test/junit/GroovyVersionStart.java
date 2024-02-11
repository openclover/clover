package org.openclover.buildutil.test.junit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface GroovyVersionStart {
    String value();
}
