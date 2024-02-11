package org.openclover.groovy.instr

import org.openclover.core.CloverDatabase
import org.openclover.core.CoverageDataSpec
import org.openclover.core.api.registry.ClassInfo
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.api.registry.PackageInfo
import org.openclover.core.registry.Clover2Registry
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo
import org.openclover.core.registry.entities.FullMethodInfo
import org.openclover.core.registry.entities.FullPackageInfo
import org.openclover.core.registry.entities.FullProjectInfo
import org.openclover.core.registry.entities.TestCaseInfo
import org.openclover.groovy.test.junit.Result
import groovy.transform.CompileStatic

import static org.openclover.groovy.utils.TestUtils.*

/**
 * Integration test that checks how Clover handles the Spock test framework. Specific issues for this framework are:
 *  - test name does not match method name (e.g def "test a == b" can be translated to $spock_feature_0_1)
 *  - the same test can be executed multiple times using different inputs, this is fine but
 *  - test name can be "unrolled" which results in different name of the test for each iteration
 *    - urolling will either add a sequence number like "my test[0]", "my test[1]"
 *    - or subsitute variables with their values like "check if #a == #b" will become "check if 5 == 0" for a=5, b=0
 *
 */
@CompileStatic
class GroovySpockTest extends TestBase {

    /** Location of sample code */
    protected File spockExampleDir = new File("src/test/resources/spock-example").getAbsoluteFile()

    protected File spockExampleSrcDir = new File(spockExampleDir, "src/test/groovy")

    /** Location of generated HTML report */
    protected File htmlReportDir

    protected boolean withJUnit5

    GroovySpockTest(String testName) {
        super(testName, testName, getGroovyJarFromProperty(),
                [ getSpockJarFromProperty(), getOpenTest4J() ])
        this.withJUnit5 = true // assuming that we run Spock 2.x by default
    }

    GroovySpockTest(String methodName, String specificName, File groovyAllJar, List<File> additionalGroovyJars,
                    boolean withJUnit5) {
        super(methodName, specificName, groovyAllJar, additionalGroovyJars)
        this.withJUnit5 = withJUnit5
    }

    private static File getSpockJarFromProperty() {
        def spockVer = System.getProperty("clover-groovy.test.spock.ver") ?: "2.3-groovy-4.0"
        new File("target/test-dependencies/spock-core-${spockVer}.jar")
    }

    private static File getOpenTest4J() {
        new File("target/test-dependencies/opentest4j-1.2.0.jar")
    }

    private static File getJUnitPlatformLauncher() {
        new File("target/test-dependencies/junit-platform-console-standalone-1.9.2.jar")
    }

    private static File getJUnitJupiterEngine() {
        new File("target/test-dependencies/junit-jupiter-engine-5.9.3.jar")
    }

    protected Result instrumentAndCompileWithGroovyAndSpock(List<String> files) {
        List sourceFiles = files.collect { new File(spockExampleSrcDir, it) }
//        String remoteDebug = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 "
        String remoteDebug = ""
        instrumentAndCompileWithGrover(sourceFiles, remoteDebug, additionalGroovyJars)
    }

    protected Result runSpockRunner(String className) {
        withJUnit5 ? runSpockRunnerWithJUnit5(className) : runSpockRunnerWithJUnit4(className)
    }

    /** Use with Spock 1.x */
    protected Result runSpockRunnerWithJUnit4(String className) {
//        String remoteDebug = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5006 "
        String remoteDebug = ""
        run("org.junit.runner.JUnitCore ${className}", [remoteDebug, "-Dclover.logging.level=debug"],
                additionalGroovyJars)
    }

    /** Use with Spock 2.x */
    protected Result runSpockRunnerWithJUnit5(String className) {
//        String remoteDebug = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5006 "
        String remoteDebug = ""
        run("org.junit.platform.console.ConsoleLauncher --select-class=${className} --disable-ansi-colors --disable-banner --details=summary",
                [remoteDebug, "-Dclover.logging.level=debug"],
                additionalGroovyJars + getOpenTest4J() + getJUnitPlatformLauncher() + getJUnitJupiterEngine())
    }

    protected Result runHtmlReport() {
//        String remoteDebug = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5006"
        String remoteDebug = ""
        htmlReportDir = new File(workingDir, "html")
        launchCmd("""
            java -classpath
            ${calcCompilationClasspath([groovyAllJar, calcRepkgJar()] + additionalGroovyJars)}
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
            assertPackage reg.model.project, { PackageInfo it -> it.isDefault() }, {FullPackageInfo p ->
                assertFile p, named("HelloSpock.groovy"), {FullFileInfo f ->
                    assertClass f, { ClassInfo it -> it.name == "HelloSpock" }, {FullClassInfo c ->

                        MethodInfo featureMethod = null
                        for (MethodInfo methodInfo : c.getMethods()) {
                            if (methodInfo.simpleName.startsWith("\$spock")) {
                                featureMethod = methodInfo
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

        assertPackage projectInfo, { PackageInfo it -> it.isDefault() }, { FullPackageInfo p ->
            assertFile p, named(FILE_NAME), { FullFileInfo f ->
                assertClass f, { ClassInfo it -> it.name == CLASS_NAME }, { FullClassInfo classInfo ->

                    // check that spock feature method exists
                    classInfo.methods.simpleName.contains(FEATURE_NAME)
                    // ... and that it contains proper static test name obtained by a DefaultTestNameExtractor
                    assertMethod classInfo, { MethodInfo it -> it.simpleName == FEATURE_NAME }, { FullMethodInfo method ->
                        "maximum of two numbers" == method.staticTestName
                    }

                    // check that three unrolled test cases were ran
                    assertEquals "number of test cases is incorrect", 3, classInfo.testCases.size()
                    // ... and that test names were dynamically updated at runtime by a TestNameSniffer
                    assertTrue(testNameContains(classInfo.testCases, "maximum of two numbers [a: 3, b: 7, c: 7, #0]"))
                    assertTrue(testNameContains(classInfo.testCases, "maximum of two numbers [a: 5, b: 4, c: 5, #1]"))
                    assertTrue(testNameContains(classInfo.testCases, "maximum of two numbers [a: 9, b: 9, c: 9, #2]"))

                    // ... and that these tests have links to the original method
                    def featureMethod = classInfo.methods.find { it.simpleName == FEATURE_NAME }
                    assertTestCase classInfo, { TestCaseInfo it -> it.testName == "maximum of two numbers [a: 3, b: 7, c: 7, #0]" }, { TestCaseInfo testInfo ->
                        featureMethod == testInfo.sourceMethod
                    }
                    assertTestCase classInfo, { TestCaseInfo it -> it.testName == "maximum of two numbers [a: 5, b: 4, c: 5, #1]" }, { TestCaseInfo testInfo ->
                        featureMethod == testInfo.sourceMethod
                    }
                    assertTestCase classInfo, { TestCaseInfo it -> it.testName == "maximum of two numbers [a: 9, b: 9, c: 9, #2]" }, { TestCaseInfo testInfo ->
                        featureMethod == testInfo.sourceMethod
                    }
                }
            }
        }
    }

    private static boolean testNameContains(Collection<TestCaseInfo> testCases, String value) {
        testCases.stream().anyMatch({ TestCaseInfo tci -> tci.testName.contains(value)})
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

        assertPackage projectInfo, { PackageInfo it -> it.isDefault() }, { FullPackageInfo p ->
            assertFile p, named(FILE_NAME), { FullFileInfo f ->
                assertClass f, { ClassInfo it -> it.name == CLASS_NAME }, { FullClassInfo classInfo ->

                    // check that spock feature method exists
                    classInfo.methods.simpleName.contains(FEATURE_NAME)
                    // ... and that it contains proper static test name obtained by a DefaultTestNameExtractor
                    assertMethod classInfo, { MethodInfo it -> it.simpleName == FEATURE_NAME }, { FullMethodInfo method ->
                        "minimum of #a and #b is #c" == method.staticTestName
                    }

                    // check that three unrolled test cases were ran
                    assertEquals 3, classInfo.testCases.size()
                    // ... and that test names were dynamically updated at runtime by a TestNameSniffer
                    assertTrue testNameContains(classInfo.testCases, "minimum of 3 and 7 is 3")
                    assertTrue testNameContains(classInfo.testCases, "minimum of 5 and 4 is 4")
                    assertTrue testNameContains(classInfo.testCases, "minimum of 9 and 9 is 9")

                    // ... and that these tests have links to the original method
                    def featureMethod = classInfo.methods.find { it.simpleName == FEATURE_NAME }
                    assertTestCase classInfo, { TestCaseInfo it -> it.testName == "minimum of 3 and 7 is 3" }, { TestCaseInfo testInfo ->
                        featureMethod == testInfo.sourceMethod
                    }
                    assertTestCase classInfo, { TestCaseInfo it -> it.testName == "minimum of 5 and 4 is 4" }, { TestCaseInfo testInfo ->
                        featureMethod == testInfo.sourceMethod
                    }
                    assertTestCase classInfo, { TestCaseInfo it -> it.testName == "minimum of 9 and 9 is 9" }, { TestCaseInfo testInfo ->
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

        assertPackage projectInfo, { PackageInfo it -> it.isDefault() }, { FullPackageInfo p ->
            assertFile p, named(FILE_NAME), { FullFileInfo f ->
                assertClass f, { ClassInfo it -> it.name == CLASS_NAME }, { FullClassInfo classInfo ->

                    // check that spock feature method exists
                    classInfo.methods.simpleName.contains(FEATURE_NAME)
                    // ... and that it contains proper static test name obtained by a DefaultTestNameExtractor
                    assertMethod classInfo, { MethodInfo it -> it.simpleName == FEATURE_NAME }, { FullMethodInfo method ->
                        "#person.name is a #sex.toLowerCase() person" == method.staticTestName
                    }

                    // check that two unrolled test cases were ran
                    assertEquals 2, classInfo.testCases.size()
                    // ... and that test names were dynamically updated at runtime by a TestNameSniffer
                    assertTrue testNameContains(classInfo.testCases, "Fred is a male person")
                    assertTrue testNameContains(classInfo.testCases, "Wilma is a female person")

                    // ... and that these tests have links to the original method
                    def featureMethod = classInfo.methods.find { it.simpleName == FEATURE_NAME }
                    assertTestCase classInfo, { TestCaseInfo it -> it.testName == "Fred is a male person" }, { TestCaseInfo testInfo ->
                        featureMethod == testInfo.sourceMethod
                    }
                    assertTestCase classInfo, { TestCaseInfo it -> it.testName == "Wilma is a female person" }, { TestCaseInfo testInfo ->
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

        instrumentAndCompileWithGroovyAndSpock(["${CLASS_NAME}.groovy".toString()])
        runSpockRunner(CLASS_NAME)
        runHtmlReport()

        // check that HelloSpock is present in the report
        File helloSpockHtml = new File(htmlReportDir, "default-pkg/${CLASS_NAME}.html")
        assertTrue helloSpockHtml.exists()

        // ... and that we've got 3 test results
        assertTestResultPage([0] as String[],
                "HelloSpock_length_of_Spock_s_and_his_friends__names__name__Kirk__length__4_.*\\.html",
                "<a  href=\"../default-pkg/HelloSpock.html?line=20#src-20\" >length of Spock's and his friends' names [name: Kirk, length: 4")
        assertTestResultPage([1] as String[],
                "HelloSpock_length_of_Spock_s_and_his_friends__names__name__Scotty__length__6_.*\\.html",
                "<a  href=\"../default-pkg/HelloSpock.html?line=20#src-20\" >length of Spock's and his friends' names [name: Scotty, length: 6")
        assertTestResultPage([2] as String[],
                "HelloSpock_length_of_Spock_s_and_his_friends__names__name__Spock__length__5_.*\\.html",
                "<a  href=\"../default-pkg/HelloSpock.html?line=20#src-20\" >length of Spock's and his friends' names [name: Spock, length: 5")
    }

    /**
     * Check if sequence-numbered tests like "my test[0]", "my test[1]" are matched to a proper method in HTML report
     */
    void testUnrolledSequentialTestsToMethodHtmlLink() {
        String CLASS_NAME = "UnrollWithSeqNumber"

        // instrument and run tests
        instrumentAndCompileWithGroovyAndSpock(["${CLASS_NAME}.groovy".toString()])
        runSpockRunner(CLASS_NAME)
        runHtmlReport()

        // check that UnrollWithVarsWithSelectors is present in the report
        File htmlSourcePage = new File(htmlReportDir, "default-pkg/${CLASS_NAME}.html")
        assertTrue htmlSourcePage.exists()

        // ... and that we've got 3 test results
        assertTestResultPage([0] as String[],
                "${CLASS_NAME}_maximum_of_two_numbers.*___0__.*\\.html",
                "<a  href=\"../default-pkg/${CLASS_NAME}.html?line=22#src-22\" >maximum of two numbers [a: 3, b: 7, c: 7, #0]</a>")
        assertTestResultPage([1] as String[],
                "${CLASS_NAME}_maximum_of_two_numbers.*___1__.*\\.html",
                "<a  href=\"../default-pkg/${CLASS_NAME}.html?line=22#src-22\" >maximum of two numbers [a: 5, b: 4, c: 5, #1]</a>")
        assertTestResultPage([2] as String[],
                "${CLASS_NAME}_maximum_of_two_numbers.*___2__.*\\.html",
                "<a  href=\"../default-pkg/${CLASS_NAME}.html?line=22#src-22\" >maximum of two numbers [a: 9, b: 9, c: 9, #2]</a>")
    }

    /**
     * Check if variable-parameterized tests like "check if #a == #b" are matched to a proper method in HTML report
     */
    void testUnrolledParameterizedTestsToMethodHtmlLink() {
        String CLASS_NAME = "UnrollWithSimpleVar"

        // instrument and run tests
        instrumentAndCompileWithGroovyAndSpock(["${CLASS_NAME}.groovy".toString()])
        runSpockRunner(CLASS_NAME)
        runHtmlReport()

        // check that UnrollWithVarsWithSelectors is present in the report
        File htmlSourcePage = new File(htmlReportDir, "default-pkg/${CLASS_NAME}.html")
        assertTrue htmlSourcePage.exists()

        // ... and that we've got 3 test results
        String[][] rows = [ [3, 7, 3] as String[], [5, 4, 4] as String[], [9, 9, 9] as String[] ]
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
    void testUnrolledParameterizedTestsWithSelectorsToHtmlLink() {
        String CLASS_NAME = "UnrollWithVarsWithSelectors"

        // instrument and run tests
        instrumentAndCompileWithGroovyAndSpock(["${CLASS_NAME}.groovy".toString()])
        runSpockRunner(CLASS_NAME)
        runHtmlReport()

        // check that UnrollWithVarsWithSelectors is present in the report
        File htmlSourcePage = new File(htmlReportDir, "default-pkg/${CLASS_NAME}.html")
        assertTrue htmlSourcePage.exists()

        // ... and that we've got 2 test results
        String[][] rows = [ ["Fred", "male"], ["Wilma", "female"] ]
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
        })

        // exactly one page for each test iteration
        assertEquals("Number of test result pages is ${resultPages.length} for inputs ${Arrays.toString(row)}",
                1, resultPages.length)

        // which href in it points to the spock feature method (file and line number)
        assertFileContains expectedLink, resultPages[0], false
    }

}