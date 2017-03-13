package com.atlassian.clover

import junit.framework.TestCase

class Log4JLoggerTest extends TestCase {
    void testLog4jSupportsLogMethod() throws ClassNotFoundException, NoSuchMethodException {
        Log4JLogger.findLogMethod()
    }
}
