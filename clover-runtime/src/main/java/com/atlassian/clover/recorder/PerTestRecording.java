package com.atlassian.clover.recorder;

public interface PerTestRecording extends CoverageRecording {
    final int FORMAT = 1;

    /**
     * Return name of the class related with the executed test. Note that name of the test class at runtime may
     * be different than name of the test class in which test was defined due to inheritance (child class may call
     * tests from a parent class).
     *
     * @return String runtime name of the test class
     */
    String getTestTypeName();

    /**
     * Return name of the test method.
     * @return String
     */
    String getTestMethodName();

    /**
     * Return name of the test which it had during execution. Some test frameworks might use a different name for the
     * test than name of the test method itself or a test name defined statically in source code (usually using
     * annotations). Examples are: @Unroll annotation for Spock, @Parameterized annotation for JUnit4.
     *
     * Might return <code>null</code> if runtime name is unknown; in such case usually fallback to {@link
     * #getTestMethodName()}
     *
     * @return String name of the test at runtime or <code>null</code> if unknown
     */
    String getRuntimeTestName();

    int getExitStatus();

    /**
     * Return time when test has started (in miliseconds, since epoch)
     * @return long datetime in miliseconds
     */
    long getStart();

    /**
     * Return time when test has ended (in miliseconds, since epoch)
     * @return long datetime in miliseconds
     */
    long getEnd();

    /**
     * Return how long test was executing (in seconds, possibly with a nanosecond precision)
     * Note: this method may return more accurate value than "getEnd()-getStart()" as, for instance,
     * the System.nanoTime() can be used for measuring it.
     * @return double duration in seconds
     */
    double getDuration();

    boolean hasResult();

    boolean isResultPassed();

    String getStackTrace();

    String getExitMessage();
}
