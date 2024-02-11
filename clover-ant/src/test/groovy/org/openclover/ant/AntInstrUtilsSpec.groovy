package org.openclover.ant

import org.openclover.core.util.FileUtils
import org.openclover.ant.testutils.RecordingLogger
import org.apache.tools.ant.Project
import org.apache.tools.ant.types.FileSet
import org.apache.tools.ant.types.Path
import org.apache.tools.ant.types.PatternSet
import spock.lang.Specification
import spock.lang.Unroll

import static org.hamcrest.core.IsCollectionContaining.hasItem
import static spock.util.matcher.HamcrestSupport.that

/**
 * Test for {@link AntInstrUtils}
 */
class AntInstrUtilsSpec extends Specification {

    static Project antProject = AntFileSetUtilsSpec.createAntProject()

    // empty source directory for Ant's PatternSet scanner - <files> tag
    static String fakeSourceRoot = "src/main/java"
    static {
        FileUtils.createEmptyDir(antProject.baseDir, fakeSourceRoot)
    }

    @Unroll
    def "test sieveSourceForInstrumentation with #caseName"() {
        setup:
        AntFileSetUtils.LOG = AntInstrUtils.LOG = new RecordingLogger()
        AntInstrUtils.sieveSourceForInstrumentation(
                antProject, srcPath, filesPattern, fileSetPatterns,
                new ArrayList(), new ArrayList(), new ArrayList())

        expect:
        // note: not using RecordingLogger.containsFragment() because it does not print buffer content
        def logMessages = ((RecordingLogger) AntInstrUtils.LOG).getBuffer() as List<RecordingLogger.LogMessage>
        that logMessages, hasItem(new RecordingLogger.MessageContainsMatcher(logMessageFragment))

        where:
        caseName << [
                "no filters",                   // all sources are instrumented, no warnings expected
                "files tag with external file", // Ant reads patterns as is, so warn about leading/trailing white space
                "files tag with atrribute",     // Ant uses stringTokenizer(", ") so it strips spaces - but warn about tabs etc
                "fileset tag with external file", // Ant reads patterns as is, so warn about leading/trailing white space
        ]
        srcPath << [
                null as Path,
                new Path(antProject, fakeSourceRoot),
                new Path(antProject, fakeSourceRoot),
                null as Path
        ]
        filesPattern << [
                null as PatternSet,
                new PatternSet(includesfile : AntFileSetUtilsSpec.createIncludesFile()),
                new PatternSet(includes : " spaces and ,s are tokenized,\ttab-before,carriage-return-after\r,\bbell\b"),
                null as PatternSet
        ]
        fileSetPatterns << [
                null as List<FileSet>,
                null as List<FileSet>,
                null as List<FileSet>,
                [ new FileSet(dir: antProject.baseDir, includesfile: AntFileSetUtilsSpec.createIncludesFile()) ]
        ]
        logMessageFragment << [
                "No <files/> nor <fileset/> elements found in <clover-setup/> - instrumenting all source found.",
                "Attention: found inclusion/exclusion patterns for " +
                        "'${new File(antProject.baseDir, fakeSourceRoot).absolutePath}' " +
                        "containing leading/trailing whitespaces:\n" +
                        "< before/**/*.java>\n" +
                        "<after/**/*.java >",
                "Attention: found inclusion/exclusion patterns for " +
                        "'${new File(antProject.baseDir, fakeSourceRoot).absolutePath}' " +
                        "containing leading/trailing whitespaces:\n" +
                        "<\ttab-before>\n" +
                        "<carriage-return-after\r>\n" +
                        "<\bbell\b>",
                "Attention: found inclusion/exclusion patterns for " +
                        "'${antProject.baseDir.absolutePath}' " +
                        "containing leading/trailing whitespaces:\n" +
                        "< before/**/*.java>\n" +
                        "<after/**/*.java >",
        ]
    }
}
