/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * http://www.apache.org/.
 */

package org.apache.tools.ant

import org.openclover.buildutil.testutils.IOHelper
import groovy.transform.CompileStatic
import junit.framework.TestCase

/**
 * A BuildFileTest is a TestCase which executes targets from an Ant buildfile 
 * for testing.
 * 
 * This class provides a number of utility methods for particular build file 
 * tests which extend this class. 
 */
@CompileStatic
abstract class BuildFileTestBase extends TestCase {
    
    protected Project project
    
    StringBuffer logBuffer
    StringBuffer fullLogBuffer
    StringBuffer outBuffer
    StringBuffer errBuffer
    BuildException buildException
    
    /**
     *  Constructor for the BuildFileTest object
     *
     *@param  name string to pass up to TestCase constructor
     */    
    BuildFileTestBase(String name) {
        super(name)
    }
    
    /**
     *  run a target, expect for any build exception 
     *
     *@param  target target to run
     *@param  cause  information string to reader of report
     */    
    protected void expectBuildException(String target, String cause) { 
        expectSpecificBuildException(target, cause, null)
    }

    /**
     * Assert that only the given message has been logged with a
     * priority &gt;= INFO when running the given target.
     */
    protected void expectLog(String target, String log) { 
        executeTarget(target)
        String realLog = getLog()
        assertEquals(log, realLog)
    }

    /**
     * Assert that the given message has been logged with a priority
     * &gt;= INFO when running the given target.
     */
    void expectLogContaining(String target, String log) {
        executeTarget(target)
        String realLog = getLog()
        assertTrue("expecting log to contain \""+log+"\" log was \""
                   + realLog + "\"",
                realLog.contains(log))
    }

    /**
     *  Gets the log the BuildFileTest object.
     *  only valid if configureProject() has
     *  been called.
     * @pre logBuffer!=null
     * @return    The log value
     */    
    protected String getLog() {
        return logBuffer.toString()
    }

    /**
     * Assert that the given message has been logged with a priority
     * &gt;= DEBUG when running the given target.
     */
    protected void expectDebuglog(String target, String log) { 
        executeTarget(target)
        String realLog = getFullLog()
        assertEquals(log, realLog)
    }

    /**
     *  Gets the log the BuildFileTest object.
     *  only valid if configureProject() has
     *  been called.
     * @pre fullLogBuffer!=null
     * @return    The log value
     */
    protected String getFullLog() {
        return fullLogBuffer.toString()
    }

    /**
     *  execute the target, verify output matches expectations
     *
     *@param  target  target to execute
     *@param  output  output to look for
     */
    
    protected void expectOutput(String target, String output) { 
        executeTarget(target)
        String realOutput = getOutput()
        assertEquals(output, realOutput)
    }

    /**
     *  execute the target, verify output matches expectations
     *  and that we got the named error at the end
     *@param  target  target to execute
     *@param  output  output to look for
     *@param  error   Description of Parameter
     */
     
    protected void expectOutputAndError(String target, String output, String error) { 
        executeTarget(target)
        String realOutput = getOutput()
        assertEquals(output, realOutput)
        String realError = getError()
        assertEquals(error, realError)
    }

    protected String getOutput() {
        return cleanBuffer(outBuffer)
    }
     
    protected String getError() {
        return cleanBuffer(errBuffer)
    }
        
    protected BuildException getBuildException() {
        return buildException
    }

    private String cleanBuffer(StringBuffer buffer) {
        if (buffer == null) {
            return ""
        }

        StringBuilder cleanedBuffer = new StringBuilder()
        boolean cr = false
        for (int i = 0; i < buffer.length(); i++) { 
            char ch = buffer.charAt(i)
            if (ch == '\r') {
                cr = true
                continue
            } else {
                cr = false
            }

            if (!cr) { 
                cleanedBuffer.append(ch)
            } else { 
                if (ch == '\n') {
                    cleanedBuffer.append(ch)
                } else {
                    cleanedBuffer.append('\r').append(ch)
                }
            }
        }
        return cleanedBuffer.toString()
    }
    
    /**
     *  set up to run the named project
     *
     * @param  filename name of project file to run
     * @param initProps (may be null)
     */    
    protected void configureProject(String filename, Map<String, String> initProps) throws BuildException {
        logBuffer = new StringBuffer()
        fullLogBuffer = new StringBuffer()
        project = new Project()
        if (initProps != null) {
            for (String name : initProps.keySet()) {
                String value = initProps.get(name)
                project.setProperty(name, value)
            }
        }
        project.init()
        project.setUserProperty( "ant.file" , new File(filename).absolutePath)
        project.setProperty("project.dir", IOHelper.projectDir.absolutePath)
        project.setSystemProperties()
        project.addBuildListener(new AntTestListener())
        ProjectHelper.configureProject(project, new File(filename))
    }

    /**
     *  execute a target we have set up
     * @pre configureProject has been called
     * @param  targetName  target to run
     */    
    void executeTarget(String targetName) {
        PrintStream sysOut = System.out
        PrintStream sysErr = System.err
        try { 
            sysOut.flush()
            sysErr.flush()
            outBuffer = new StringBuffer()
            PrintStream out = new PrintStream(new AntOutputStream())
            System.setOut(out)
            errBuffer = new StringBuffer()
            PrintStream err = new PrintStream(new AntOutputStream())
            System.setErr(err)
            logBuffer = new StringBuffer()
            fullLogBuffer = new StringBuffer()
            buildException = null

            DefaultLogger logger = new DefaultLogger()

            logger.setMessageOutputLevel(Project.MSG_DEBUG)
            logger.setOutputPrintStream(sysOut)
            logger.setErrorPrintStream(sysOut)

            project.addBuildListener(logger)
            project.executeTarget(targetName)
        } finally {
            System.setOut(sysOut)
            System.setErr(sysErr)
        }
        
    }

    /**
     * Get the project which has been configured for a test.
     *
     * @return the Project instance for this test.
     */
    Project getProject() {
        return project
    }

    /**
     * get the directory of the project
     * @return the base dir of the project
     */
    protected File getProjectDir() {
        return project.getBaseDir()
    }

    /**
     * Converts Windows/Unix/MacOS line endings in input string into line endings used on current platform
     * (line.separator).
     *
     * @param input
     * @return
     */
    protected String sanitizeLineEndings(String input) {
        // Windows to Unix, Mac to Unix, Unix to line.separator
        return input == null ? null : input.replace("\r\n", "\n").replace("\r", "\n").replace("\n", System.getProperty("line.separator"))
    }

    protected String normalizeWhitespace(String input) {
        // remove newline characters and use a single space character for any space/tab sequence
        return input.replace("\r", "").replace("\n", "").replaceAll("[ \t]+", " ")
    }

    void assertPropertyContains(String prop1, String prop2) throws IOException {
        assertPropertyContains(prop1, prop2, null)
    }

    void assertPropertyContains(String prop1, String prop2, File errorOut) throws IOException {

        final String val1 = normalizeWhitespace(getProject().getProperty(prop1))
        final String val2 = normalizeWhitespace(getProject().getProperty(prop2))

        assertNotNull(val1)
        assertNotNull(val2)

        if (val1.contains(val2)) {
            return
        }
        String fileLoc = ""

        if (errorOut != null) {
            FileWriter out = new FileWriter(errorOut)
            out.write(val1)
            out.close()
            fileLoc = "Actual source at " + errorOut.getAbsolutePath() + "  "
        }


        if (val1.toLowerCase().contains(val2.toLowerCase())) {
            fail(prop1 + " contains " + prop2 + " contents, except has different CASE. \n" + fileLoc)
        }

        fail(prop1 + " doesn't contain " + prop2 + ".\n" + prop2 + " = '" + val2 + "' " +  fileLoc)
    }

    /**
     * assert that a property equals a value; comparison is case sensitive.
     * @param property property name
     * @param value expected value
     */
    protected void assertPropertyEquals(String property, String value) {
        String result = project.getProperty(property)
        assertEquals("property " + property,value,result)
    }

    /**
     * assert that a property equals &quot;true&quot
     * @param property property name
     */
    protected void assertPropertySet(String property) {
        assertPropertyEquals(property, "true")
    }

    /**
     * assert that a property is null
     * @param property property name
     */
    protected void assertPropertyUnset(String property) {
        assertPropertyEquals(property, null)
    }

    void assertAntOutputContains(String message) {
        String output = getOutput()

        assertTrue("Expecting ant output to contain \"" + message + "\", but it was \""
                + output + "\"",
                output.contains(message))
    }

    void assertLogContains(String logMessage) {
        String log = getLog()
        assertTrue("Expecting log to contain \"" + logMessage + "\", but it was \""
                + log + "\"",
                log.contains(logMessage))
    }

    void assertFullLogContains(String logMessage) {
        assertFullLogContains(logMessage, 1)
    }

    void assertFullLogContains(String logMessage, int count) {
        String log = getFullLog()
        int lastIndex = -1, i = 0

        for (lastIndex = log.indexOf(logMessage, lastIndex + 1);
             lastIndex != -1 && i < count;
             lastIndex = log.indexOf(logMessage, lastIndex + 1)) {
            i++
        }

        assertTrue("Expecting full log to contain " + (count > 1 ? "at least ${count} times " : "")
                + "\"${logMessage}\", but it was \"${log}\"",
                i >= count)
    }

    /**
     *  run a target, wait for a build exception 
     *
     *@param  target target to run
     *@param  cause  information string to reader of report
     *@param  msg    the message value of the build exception we are waiting for
              set to null for any build exception to be valid
     */    
    protected void expectSpecificBuildException(String target, String cause, String msg) { 
        try {
            executeTarget(target)
        } catch (org.apache.tools.ant.BuildException ex) {
            buildException = ex
            if ((null != msg) && (!ex.getMessage().equals(msg))) {
                fail("Should throw BuildException because '" + cause
                        + "' with message '" + msg
                        + "' (actual message '" + ex.getMessage() + "' instead)")
            }
            return
        }
        fail("Should throw BuildException because: " + cause)
    }
    
    /**
     *  run a target, expect an exception string
     *  containing the substring we look for (case sensitive match)
     *
     *@param  target target to run
     *@param  cause  information string to reader of report
     *@param  contains  substring of the build exception to look for
     */
    protected void expectBuildExceptionContaining(String target, String cause, String contains) { 
        try {
            executeTarget(target)
        } catch (org.apache.tools.ant.BuildException ex) {
            buildException = ex
            if ((null != contains) && (!ex.getMessage().contains(contains))) {
                fail("Should throw BuildException because '" + cause +
                        "' with message containing '" + contains +
                        "' (actual message '" + ex.getMessage() + "' instead)")
            }
            return
        }
        fail("Should throw BuildException because: " + cause)
    }
    

    /**
     * call a target, verify property is as expected
     *
     * @param target build file target
     * @param property property name
     * @param value expected value
     */

    protected void expectPropertySet(String target, String property, String value) {
        executeTarget(target)
        assertPropertyEquals(property, value)
    }

    /**
     * call a target, verify named property is "true".
     *
     * @param target build file target
     * @param property property name
     */
    protected void expectPropertySet(String target, String property) {
        expectPropertySet(target, property, "true")
    }


    /**
     * call a target, verify property is null
     * @param target build file target
     * @param property property name
     */
    protected void expectPropertyUnset(String target, String property) {
        expectPropertySet(target, property, null)
    }

    /**
     * Retrieve a resource from the caller classloader to avoid
     * assuming a vm working directory. The resource path must be
     * relative to the package name or absolute from the root path.
     * @param resource the resource to retrieve its url.
     * @throws AssertionFailureException if resource is not found.
     */
    protected URL getResource(String resource){
        URL url = getClass().getResource(resource)
        assertNotNull("Could not find resource :" + resource, url)
        return url
    }

    /**
     * an output stream which saves stuff to our buffer.
     */
    private class AntOutputStream extends java.io.OutputStream {
        void write(int b) { 
            outBuffer.append((char)b)
        }
    }

    /**
     * our own personal build listener
     */
    private class AntTestListener implements BuildListener {
        /**
         *  Fired before any targets are started.
         */
        void buildStarted(BuildEvent event) {
        }

        /**
         *  Fired after the last target has finished. This event
         *  will still be thrown if an error occured during the build.
         *
         *  @see BuildEvent#getException()
         */
        void buildFinished(BuildEvent event) {
        }

        /**
         *  Fired when a target is started.
         *
         *  @see BuildEvent#getTarget()
         */
        void targetStarted(BuildEvent event) {
            //System.out.println("targetStarted " + event.getTarget().getName())
        }

        /**
         *  Fired when a target has finished. This event will
         *  still be thrown if an error occured during the build.
         *
         *  @see BuildEvent#getException()
         */
        void targetFinished(BuildEvent event) {
            //System.out.println("targetFinished " + event.getTarget().getName())
        }

        /**
         *  Fired when a task is started.
         *
         *  @see BuildEvent#getTask()
         */
        void taskStarted(BuildEvent event) {
            //System.out.println("taskStarted " + event.getTask().getTaskName())
        }

        /**
         *  Fired when a task has finished. This event will still
         *  be throw if an error occured during the build.
         *
         *  @see BuildEvent#getException()
         */
        void taskFinished(BuildEvent event) {
            //System.out.println("taskFinished " + event.getTask().getTaskName())
        }

        /**
         *  Fired whenever a message is logged.
         *
         *  @see BuildEvent#getMessage()
         *  @see BuildEvent#getPriority()
         */
        void messageLogged(BuildEvent event) {
            // The logBuffer gets only messages with priority >= MSG_INFO
            if (event.getPriority() == Project.MSG_INFO ||
                event.getPriority() == Project.MSG_WARN ||
                event.getPriority() == Project.MSG_ERR) {
                logBuffer.append(event.getMessage()).append("\n")
            }
            // The fullLogBuffer gets everything
            fullLogBuffer.append(event.getMessage()).append("\n")
            System.out.println("[ANTUNIT] " + event.getMessage())
        }
    }


}
