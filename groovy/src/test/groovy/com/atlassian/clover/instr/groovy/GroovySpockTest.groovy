package com.atlassian.clover.instr.groovy

import com.atlassian.clover.CloverDatabase
import com.atlassian.clover.CoverageDataSpec
import com.atlassian.clover.api.registry.MethodInfo
import com.atlassian.clover.registry.Clover2Registry
import com.atlassian.clover.registry.entities.FullClassInfo
import com.atlassian.clover.registry.entities.FullFileInfo
import com.atlassian.clover.registry.entities.FullMethodInfo
import com.atlassian.clover.registry.entities.FullPackageInfo
import com.atlassian.clover.registry.entities.FullProjectInfo
import com.atlassian.clover.registry.entities.TestCaseInfo
import com.atlassian.clover.test.junit.GroovyVersionStart
import com.atlassian.clover.test.junit.Result

import static com.atlassian.clover.TestUtils.*

/**
 * Integration test that checks how Clover handles the Spock test framework. Specific issues for this framework are:
 *  - test name does not match method name (e.g def "test a == b" can be translated to $spock_feature_0_1)
 *  - the same test can be executed multiple times using different inputs, this is fine but
 *  - test name can be "unrolled" which results in different name of the test for each iteration
 *    - urolling will either add a sequence number like "my test[0]", "my test[1]"
 *    - or subsitute variables with their values like "check if #a == #b" will become "check if 5 == 0" for a=5, b=0
 *
 */
class GroovySpockTest extends TestBase {

    /** Location of sample code */
    protected File spockExampleDir = new File(getFileProp("project.dir"), "groovy/src/test/resources/spock-example")

    protected File spockExampleSrcDir = new File(spockExampleDir, "src/test/groovy")

    /** Location of generated HTML report */
    protected File htmlReportDir

    /** Location of Spock framework JAR to test against */
    protected File spockJar

    GroovySpockTest(String testName) {
        super(testName);
        spockJar = new File(getFileProp("project.dir"), "target/dependencies/spock-core-0.7-groovy-2.0.jar")
    }

    GroovySpockTest(methodName, specificName, groovyAllJar, spockJar) {
        super(methodName, specificName, groovyAllJar)
        this.spockJar = spockJar
    }

    protected Result instrumentAndCompileWithGroovyAndSpock(List<String> files) {
        List sourceFiles = files.collect { new File(spockExampleSrcDir, it) }
//        String remoteDebug = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 "
        String remoteDebug = ""
        instrumentAndCompileWithGrover(sourceFiles, remoteDebug, [spockJar.getAbsolutePath()])
    }

    protected Result runSpockRunner(String className) {
//        String remoteDebug = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5006 "
        String remoteDebug = ""
        run("org.junit.runner.JUnitCore ${className}", [remoteDebug, "-Dclover.logging.level=debug"], [spockJar.getAbsolutePath()])
    }

    protected Result runHtmlReport() {
//        String remoteDebug = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5006"
        String remoteDebug = ""
        htmlReportDir = new File(workingDir, "html")
        launchCmd("""
            java -classpath
            ${calcCompilationClasspath([groovyAllJar.getAbsolutePath(), calcRepkgJarPath()])}
            ${remoteDebug}
            com.atlassian.clover.reporters.html.HtmlReporter -i ${db.absolutePath} -o ${htmlReportDir} -a -e
        """)
    }

    /**
     * Check if test name like "my test" matches to proper method name in a class
     */
    void testTestNameToMethodName() {
        instrumentAndCompileWithGroovyAndSpock(["HelloSpock.groovy"])
        runSpockRunner("HelloSpock")

        assertRegistry db, { Clover2Registry reg ->
            assertPackage reg.model.project, { it.isDefault() }, {FullPackageInfo p ->
                assertFile p, named("HelloSpock.groovy"), {FullFileInfo f ->
                    assertClass f, { it.name == "HelloSpock" }, {FullClassInfo c ->

                        MethodInfo featureMethod = null;
                        for (MethodInfo methodInfo : c.getMethods()) {
                            if (methodInfo.simpleName.startsWith("\$spock")) {
                                featureMethod = methodInfo;
                            }
                        }

                        // the following will be called as assertion
                        (featureMethod != null) &&
                                ("\$spock_feature_0_0" == featureMethod.simpleName) &&
                                ("length of Spock's and his friends' names" == featureMethod.staticTestName)
                    }
                }
            }
        }
    }

    /**
     * Check if sequence-numbered tests like "my test[0]", "my test[1]" are matched to a method name in class
     */
    void testUnrolledSequentialTestsToMethodName() {
        final String FILE_NAME = "UnrollWithSeqNumber.groovy"
        final String CLASS_NAME = "UnrollWithSeqNumber"
        String FEATURE_NAME = "\$spock_feature_0_0"

        // instrument and run tests
        instrumentAndCompileWithGroovyAndSpock([FILE_NAME])
        runSpockRunner(CLASS_NAME)

        FullProjectInfo projectInfo = CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getFullModel()

        assertPackage projectInfo, { it.isDefault() }, { FullPackageInfo p ->
            assertFile p, named(FILE_NAME), { FullFileInfo f ->
                assertClass f, { it.name == CLASS_NAME }, { FullClassInfo classInfo ->

                    // check that spock feature method exists
                    classInfo.methods.simpleName.contains(FEATURE_NAME)
                    // ... and that it contains proper static test name obtained by a DefaultTestNameExtractor
                    assertMethod classInfo, { it.simpleName == FEATURE_NAME }, { FullMethodInfo method ->
                        "maximum of two numbers" == method.staticTestName
                    }

                    // check that three unrolled test cases were ran
                    assertEquals "number of test cases is incorrect", 3, classInfo.testCases.size()
                    // ... and that test names were dynamically updated at runtime by a TestNameSniffer
                    assertTrue classInfo.testCases.testName.contains("maximum of two numbers[0]")
                    assertTrue classInfo.testCases.testName.contains("maximum of two numbers[1]")
                    assertTrue classInfo.testCases.testName.contains("maximum of two numbers[2]")

                    // ... and that these tests have links to the original method
                    def featureMethod = classInfo.methods.find { it.simpleName == FEATURE_NAME }
                    assertTestCase classInfo, { it.testName == "maximum of two numbers[0]" }, { TestCaseInfo testInfo ->
                        featureMethod == testInfo.sourceMethod
                    }
                    assertTestCase classInfo, { it.testName == "maximum of two numbers[1]" }, { TestCaseInfo testInfo ->
                        featureMethod == testInfo.sourceMethod
                    }
                    assertTestCase classInfo, { it.testName == "maximum of two numbers[2]" }, { TestCaseInfo testInfo ->
                        featureMethod == testInfo.sourceMethod
                    }
                }
            }
        }
    }

    /**
     * Check if variable-parameterized tests like "check if #a == #b" are matched to a method name in class
     */
    void testUnrolledParameterizedTestsToMethodName() {
        String FILE_NAME = "UnrollWithSimpleVar.groovy"
        String CLASS_NAME = "UnrollWithSimpleVar"
        String FEATURE_NAME = "\$spock_feature_0_0"

        // instrument and run tests
        instrumentAndCompileWithGroovyAndSpock([FILE_NAME])
        runSpockRunner(CLASS_NAME)

        FullProjectInfo projectInfo = CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getFullModel()

        assertPackage projectInfo, { it.isDefault() }, { FullPackageInfo p ->
            assertFile p, named(FILE_NAME), { FullFileInfo f ->
                assertClass f, { it.name == CLASS_NAME }, { FullClassInfo classInfo ->

                    // check that spock feature method exists
                    classInfo.methods.simpleName.contains(FEATURE_NAME)
                    // ... and that it contains proper static test name obtained by a DefaultTestNameExtractor
                    assertMethod classInfo, { it.simpleName == FEATURE_NAME }, { FullMethodInfo method ->
                        "minimum of #a and #b is #c" == method.staticTestName
                    }

                    // check that three unrolled test cases were ran
                    assertEquals 3, classInfo.testCases.size()
                    // ... and that test names were dynamically updated at runtime by a TestNameSniffer
                    assertTrue classInfo.testCases.testName.contains("minimum of 3 and 7 is 3")
                    assertTrue classInfo.testCases.testName.contains("minimum of 5 and 4 is 4")
                    assertTrue classInfo.testCases.testName.contains("minimum of 9 and 9 is 9")

                    // ... and that these tests have links to the original method
                    def featureMethod = classInfo.methods.find { it.simpleName == FEATURE_NAME }
                    assertTestCase classInfo, { it.testName == "minimum of 3 and 7 is 3" }, { TestCaseInfo testInfo ->
                        featureMethod == testInfo.sourceMethod
                    }
                    assertTestCase classInfo, { it.testName == "minimum of 5 and 4 is 4" }, { TestCaseInfo testInfo ->
                        featureMethod == testInfo.sourceMethod
                    }
                    assertTestCase classInfo, { it.testName == "minimum of 9 and 9 is 9" }, { TestCaseInfo testInfo ->
                        featureMethod == testInfo.sourceMethod
                    }
                }
            }
        }
    }

    /**
     * Check if variable-parameterized tests containing variables with fields or method calls are matched to
     * a method name in a class.
     */
    void testUnrolledParameterizedTestsWithSelectorsToMethodName() {
        String FILE_NAME = "UnrollWithVarsWithSelectors.groovy"
        String CLASS_NAME = "UnrollWithVarsWithSelectors"
        String FEATURE_NAME = "\$spock_feature_0_0"

        // instrument and run tests
        instrumentAndCompileWithGroovyAndSpock([FILE_NAME])
        runSpockRunner(CLASS_NAME)

        FullProjectInfo projectInfo = CloverDatabase.loadWithCoverage(db.absolutePath, new CoverageDataSpec()).getFullModel()

        assertPackage projectInfo, { it.isDefault() }, { FullPackageInfo p ->
            assertFile p, named(FILE_NAME), { FullFileInfo f ->
                assertClass f, { it.name == CLASS_NAME }, { FullClassInfo classInfo ->

                    // check that spock feature method exists
                    classInfo.methods.simpleName.contains(FEATURE_NAME)
                    // ... and that it contains proper static test name obtained by a DefaultTestNameExtractor
                    assertMethod classInfo, { it.simpleName == FEATURE_NAME }, { FullMethodInfo method ->
                        "#person.name is a #sex.toLowerCase() person" == method.staticTestName
                    }

                    // check that two unrolled test cases were ran
                    assertEquals 2, classInfo.testCases.size()
                    // ... and that test names were dynamically updated at runtime by a TestNameSniffer
                    assertTrue classInfo.testCases.testName.contains("Fred is a male person")
                    assertTrue classInfo.testCases.testName.contains("Wilma is a female person")

                    // ... and that these tests have links to the original method
                    def featureMethod = classInfo.methods.find { it.simpleName == FEATURE_NAME }
                    assertTestCase classInfo, { it.testName == "Fred is a male person" }, { TestCaseInfo testInfo ->
                        featureMethod == testInfo.sourceMethod
                    }
                    assertTestCase classInfo, { it.testName == "Wilma is a female person" }, { TestCaseInfo testInfo ->
                        featureMethod == testInfo.sourceMethod
                    }
                }
            }
        }
    }


    /**
     * Check if test name like "my test" on "Tests" page links to proper method in source page in HTML report
     */
    void testTestNameToMethodNameHtmlLink() {
        String CLASS_NAME = "HelloSpock"

        instrumentAndCompileWithGroovyAndSpock(["${CLASS_NAME}.groovy"])
        runSpockRunner(CLASS_NAME)
        runHtmlReport()

        // check that HelloSpock is present in the report
        File helloSpockHtml = new File(htmlReportDir, "default-pkg/${CLASS_NAME}.html")
        assertTrue helloSpockHtml.exists()

        // ... and that we've got 3 test results
        for (int i = 0; i < 3; i++) {
            String[] row = [ i ]
            assertTestResultPage(row,
                    "${CLASS_NAME}_length_of_Spock_s_and_his_friends__names_${i}.html",
                    "<a  href=\"../default-pkg/HelloSpock.html?line=20#src-20\" >length of Spock's and his friends' names</a>")
        }
    }

    /**
     * Check if sequence-numbered tests like "my test[0]", "my test[1]" are matched to a proper method in HTML report
     */
    void testUnrolledSequentialTestsToMethodHtmlLink() {
        String CLASS_NAME = "UnrollWithSeqNumber"

        // instrument and run tests
        instrumentAndCompileWithGroovyAndSpock(["${CLASS_NAME}.groovy"])
        runSpockRunner(CLASS_NAME)
        runHtmlReport()

        // check that UnrollWithVarsWithSelectors is present in the report
        File htmlSourcePage = new File(htmlReportDir, "default-pkg/${CLASS_NAME}.html")
        assertTrue htmlSourcePage.exists()

        // ... and that we've got 3 test results
        for (int i = 0; i < 3; i++) {
            String[] row = [ i ]
            assertTestResultPage(row,
                    "${CLASS_NAME}_maximum_of_two_numbers_${row[0]}__[0-9]+\\.html",
                    "<a  href=\"../default-pkg/${CLASS_NAME}.html?line=22#src-22\" >maximum of two numbers[${row[0]}]</a>")
        }
    }

    /**
     * Check if variable-parameterized tests like "check if #a == #b" are matched to a proper method in HTML report
     */
    void testUnrolledParameterizedTestsToMethodHtmlLink() {
        String CLASS_NAME = "UnrollWithSimpleVar"

        // instrument and run tests
        instrumentAndCompileWithGroovyAndSpock(["${CLASS_NAME}.groovy"])
        runSpockRunner(CLASS_NAME)
        runHtmlReport()

        // check that UnrollWithVarsWithSelectors is present in the report
        File htmlSourcePage = new File(htmlReportDir, "default-pkg/${CLASS_NAME}.html")
        assertTrue htmlSourcePage.exists()

        // ... and that we've got 3 test results
        String[][] rows = [ [3, 7, 3], [5, 4, 4], [9, 9, 9]];
        for (String[] row : rows) {
            assertTestResultPage(row,
                    "${CLASS_NAME}_minimum_of_${row[0]}_and_${row[1]}_is_${row[2]}_[0-9]+\\.html",
                    "<a  href=\"../default-pkg/${CLASS_NAME}.html?line=22#src-22\" >minimum of ${row[0]} and ${row[1]} is ${row[2]}</a>")
        }
    }

    /**
     * Check if variable-parameterized tests containing variables with fields or method calls are matched to
     * a proper method in HTML report.
     */
    @GroovyVersionStart("1.8.0")
    void testUnrolledParameterizedTestsWithSelectorsToHtmlLink() {
        String CLASS_NAME = "UnrollWithVarsWithSelectors"

        // instrument and run tests
        instrumentAndCompileWithGroovyAndSpock(["${CLASS_NAME}.groovy"])
        runSpockRunner(CLASS_NAME)
        runHtmlReport()

        // check that UnrollWithVarsWithSelectors is present in the report
        File htmlSourcePage = new File(htmlReportDir, "default-pkg/${CLASS_NAME}.html")
        assertTrue htmlSourcePage.exists()

        // ... and that we've got 2 test results
        String[][] rows = [ ["Fred", "male"], ["Wilma", "female"] ];
        for (String[] row : rows) {
            assertTestResultPage(row,
                    "${CLASS_NAME}_${row[0]}_is_a_${row[1]}_person_[0-9]+\\.html",
                    "<a  href=\"../default-pkg/${CLASS_NAME}.html?line=29#src-29\" >${row[0]} is a ${row[1]} person</a>")
        }
    }

    /**
     * Validate that we have a correct test result page in our HTML report
     *
     * @param row   row with input parameters which were used for test
     * @param resultPageName  regular expression for matching file name with test results
     * @param expectedLink    exact string which shall be present in the content of the test result page
     */
    void assertTestResultPage(String[] row, String resultPageName, String expectedLink) {
        // find html report page for each row of input data
        File[] resultPages = new File(htmlReportDir, "default-pkg").listFiles(new FilenameFilter() {
            boolean accept(File dir, String name) {
                name.matches(resultPageName)
            }
        });

        // exactly one page for each test iteration
        assertEquals("Number of test result pages is ${resultPages.length} for inputs ${Arrays.toString(row)}",
                1, resultPages.length)

        // which href in it points to the spock feature method (file and line number)
        assertFileContains expectedLink, resultPages[0], false
    }

}