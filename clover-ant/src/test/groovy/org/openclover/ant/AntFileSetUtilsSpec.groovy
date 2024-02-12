package org.openclover.ant

import org.apache.tools.ant.Project
import org.apache.tools.ant.types.FileSet
import org.apache.tools.ant.types.PatternSet
import org.openclover.ant.testutils.RecordingLogger
import org.openclover.core.util.FileUtils
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Test for {@link AntFileSetUtils}
 */
class AntFileSetUtilsSpec extends Specification {

    @Unroll
    def "test getNonTrimmedPatterns"() {
        setup:
        def list = AntFileSetUtils.getNonTrimmedPatterns(incPatterns, exclPatterns)

        expect:
        list == expectedList

        where:
        incPatterns                         | exclPatterns                      || expectedList
        null as String[]                    | null as String[]                  || [] // nulls
        null as String[]                    | ["abc "] as String[]              || ["abc "] // nulls
        ["abc "] as String[]                | null as String[]                  || ["abc "] // nulls
        [""] as String[]                    | [""] as String[]                  || [] // empty strings
        ["a", "b"] as String[]              | ["c", "d", "e"] as String[]       || []  // non-null patterns, no whitespace
        ["in the middle", ""] as String[]   | [" before", "after "] as String[] || [" before", "after "] // whitespace, check only leading/trailing
        ["\t", "\r", "\n", " "] as String[] | [""] as String[]                  || ["\t", "\r", "\n", " "] // whitespace only
    }

    def "test createNonTrimmedPatternsMessage"() {
        expect:
        def message = AntFileSetUtils.createNonTrimmedPatternsMessage(
                [" leading/**/*.space", "trailing/**/*.space "],
                "source/dir"
        )
        message == "Attention: found inclusion/exclusion patterns for 'source/dir' containing leading/trailing whitespaces:\n" +
                "< leading/**/*.space>\n" +
                "<trailing/**/*.space >"
    }

    // temporary file containing inclusion patterns
    static File createIncludesFile() {
        File file = File.createTempFile("AntFileSetUtilsSpec_createIncludesFile", ".txt")
        file.deleteOnExit()
        file.write(" before/**/*.java\nafter/**/*.java ")
        file
    }

    static Project createAntProject() {
        File fakeDir = FileUtils.createTempDir("AntFileSetUtils_createAntProject")
        new Project(baseDir: fakeDir)
    }

    def "test checkForNonTrimmedPatterns"() {
        setup:
        Project antProject = createAntProject()
        String logMessageFragment =
                "Attention: found inclusion/exclusion patterns for '${antProject.baseDir.absolutePath}' containing leading/trailing whitespaces:\n" +
                "< before/**/*.java>\n" +
                "<after/**/*.java >"
        RecordingLogger logger = new RecordingLogger()
        AntFileSetUtils.LOG = logger

        expect:
        // test with PatternSet
        AntFileSetUtils.checkForNonTrimmedPatterns(
                new PatternSet(includesfile: createIncludesFile()),
                antProject,
                antProject.baseDir.absolutePath
        )
        logger.containsFragment(logMessageFragment) == true // assertion

        // test with FileSet
        logger.reset()
        AntFileSetUtils.checkForNonTrimmedPatterns(
                new FileSet(dir: antProject.baseDir, includesfile: createIncludesFile()),
                antProject
        )
        logger.containsFragment(logMessageFragment) == true // assertion
    }

}
