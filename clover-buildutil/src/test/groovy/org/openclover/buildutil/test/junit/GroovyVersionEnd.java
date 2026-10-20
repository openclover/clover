package org.openclover.buildutil.test.junit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Runs the test only for Groovy versions lower than the given (exclusive) one.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface GroovyVersionEnd {
    String value();
}
