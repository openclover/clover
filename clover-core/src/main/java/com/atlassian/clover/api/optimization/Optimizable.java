package com.atlassian.clover.api.optimization;

/**
 * A {@link Optimizable} is an abstraction for things that can be executed for the purpose of testing
 * and can be potentially optimized away by Clover.
 * <p/>
 * Examples include Ant <code>org.apache.tools.ant.types.Resource</code>s that map to test files
 * or instances of <code>org.apache.tools.ant.taskdefs.optional.junit.JUnitTest</code> within Ant.
 * <p/>
 * {@link Optimizable}s have a name ({@link #getName()}) which may or may not relate to a path. If
 * relating to a path, the path separator '/' must be used instead of the platform path separator.
 *
 */
public interface Optimizable {
    /** @return non-null name */
    String getName();
}
