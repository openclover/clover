package com.atlassian.clover.idea.junit;

import com.atlassian.clover.idea.junit.config.OptimizedConfigurationSettings;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.testFramework.LightIdeaTestCase;

import java.io.File;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test for {@link JUnitOptimizingProgramRunnerIdea13}
 */
public class JUnitOptimizingProgramRunnerIdea13Test extends LightIdeaTestCase {

    private static final String[] POSITIVE_TEST_CASES = {
            "@TMP",
            "p1 @TMP",
            "@@listener/file @TMP",
            "@TMP @different/one"
    };

    private static final String[] NEGATIVE_TEST_CASES = {
            "",
            "parameter",
            "parameter1 parameter2",
            "param1 @@listener/file",
            "@",
            "@ param",
            "param @",
            "@@",
            "@w@/somefiledir.tmp"
    };

    private final JUnitOptimizingProgramRunner optimizingProgramRunner = new JUnitOptimizingProgramRunnerIdea13();

    private File tmpFile;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        tmpFile = File.createTempFile("clover_test", ".tmp");
        tmpFile.deleteOnExit();
    }

    @Override
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public void tearDown() throws Exception {
        tmpFile.delete();
        super.tearDown();
    }

    public void testRetrieveTmpFile() throws Exception {
        for (String testCase : NEGATIVE_TEST_CASES) {
            verifyRetrieveTmpFileResult(null, testCase);
        }

        for (String testCase : POSITIVE_TEST_CASES) {
            final String paramString = testCase.replace("TMP", tmpFile.getPath());
            verifyRetrieveTmpFileResult(tmpFile, paramString);
        }
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public void testRetrieveNonExistentFile() throws Exception {
        tmpFile.delete();
        for (String testCase : POSITIVE_TEST_CASES) {
            final String paramString = testCase.replace("TMP", tmpFile.getPath());
            verifyRetrieveTmpFileResult(null, paramString);
        }
    }

    @SuppressWarnings({"MagicNumber"})
    public void testRetrieveSynchSocket() throws Exception {
        verifyRetrieveSynchSocket(-1, "");
        verifyRetrieveSynchSocket(-1, "-ideVersion5 @/tmp/idea_junit665630085301222824.tmp");
        verifyRetrieveSynchSocket(1234, "-socket1234");
        verifyRetrieveSynchSocket(36162, "-ideVersion5 @/tmp/idea_junit665630085301222824.tmp -socket36162");
    }

    @SuppressWarnings({"MagicNumber"})
    public void testReplaceTmpFile() throws Exception {
        final JavaParameters javaParameters = new JavaParameters();
        final ParametersList parametersList = javaParameters.getProgramParametersList();
        parametersList.addParametersString("-ideVersion5 @/tmp/idea_junit665630085301222824.tmp -socket36162");

        assertEquals(36162, optimizingProgramRunner.retrieveJUnitSychSocket(javaParameters));
        optimizingProgramRunner.replaceJUnitSynchSocket(javaParameters, 7777);

        assertEquals(7777, optimizingProgramRunner.retrieveJUnitSychSocket(javaParameters));
        // Note: we use trim() because IDEA [8.x-10.x] puts space character at the beginning, while [11.x] not
        assertEquals("-ideVersion5 @/tmp/idea_junit665630085301222824.tmp -socket7777", javaParameters.getProgramParametersList().getParametersString().trim());
    }


    @SuppressWarnings({"MagicNumber"})
    public void testReplaceSynchSocket() throws Exception {
        verifyRetrieveSynchSocket(-1, "");
        verifyRetrieveSynchSocket(-1, "-ideVersion5 @/tmp/idea_junit665630085301222824.tmp");
        verifyRetrieveSynchSocket(1234, "-socket1234");
        verifyRetrieveSynchSocket(36162, "-ideVersion5 @/tmp/idea_junit665630085301222824.tmp -socket36162");
    }

    /**
     * Test that in IDEA13 or newer we get configuration object of a proper type
     */
    public void testCreateConfigurationDataSinceIdea13() throws Exception {
        final RunnerSettings data = optimizingProgramRunner.createConfigurationData(null);
        assertNotNull(data);
        assertThat(data, instanceOf(RunnerSettings.class));
        assertThat(data, instanceOf(OptimizedConfigurationSettings.class));
    }

    private void verifyRetrieveTmpFileResult(File expected, String paramString) {
        final JavaParameters javaParameters = new JavaParameters();
        final ParametersList parametersList = javaParameters.getProgramParametersList();
        parametersList.addParametersString(paramString);
        final File result = optimizingProgramRunner.retrieveTmpFile(javaParameters);
        assertEquals(expected, result);
    }

    private void verifyRetrieveSynchSocket(int expected, String paramString) {
        final JavaParameters javaParameters = new JavaParameters();
        final ParametersList parametersList = javaParameters.getProgramParametersList();
        parametersList.addParametersString(paramString);
        final int result = optimizingProgramRunner.retrieveJUnitSychSocket(javaParameters);
        assertEquals(expected, result);
    }

}
