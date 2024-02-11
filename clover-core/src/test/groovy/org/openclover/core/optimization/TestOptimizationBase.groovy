package org.openclover.core.optimization

import com.atlassian.clover.instr.InstrumentationSessionImpl
import org.openclover.runtime.ErrorInfo
import com.atlassian.clover.context.ContextSet
import com.atlassian.clover.registry.Clover2Registry
import com.atlassian.clover.registry.entities.FullMethodInfo
import org.openclover.buildutil.testutils.IOHelper
import org_openclover_runtime.CoverageRecorder
import org.junit.Rule
import org.junit.rules.TestName

abstract class TestOptimizationBase {
    protected static final String THIS_PACKAGE = "com.acme"
    protected static final String TEST_MAIN_METHOD_SUFFIX = ".testMain"

    protected File tmpDir
    protected FullMethodInfo appClass1_main
    protected FullMethodInfo appClass2_main
    protected FullMethodInfo appClass3_main
    protected FullMethodInfo appClass4_main
    protected FullMethodInfo noAppClassTest_testMain
    protected FullMethodInfo appClass2Test_testMain
    protected FullMethodInfo appClass23Test_testMain
    protected FullMethodInfo appClass234Test_testMain
    private FullMethodInfo failingTest_testFail
    protected Clover2Registry registry
    protected CoverageRecorder recorder
    protected int testID
    protected int fileSize

    @Rule
    public TestName testName = new TestName()

    protected void baseSetUp() throws Exception {
        tmpDir = IOHelper.createTmpDir(testName.getMethodName())
        File registryFile = File.createTempFile("registry", ".cdb", tmpDir)

        registry = new Clover2Registry(registryFile, testName.getMethodName())
        ContextSet context = new ContextSet()
        final InstrumentationSessionImpl session = (InstrumentationSessionImpl) registry.startInstr()

        fileSize = 100
        appClass1_main = addClassWithSingleMethod(session, context, THIS_PACKAGE, "AppClass1", "main", false)
        appClass2_main = addClassWithSingleMethod(session, context, THIS_PACKAGE, "AppClass2", "main", false)
        appClass3_main = addClassWithSingleMethod(session, context, THIS_PACKAGE, "AppClass3", "main", false)
        appClass4_main = addClassWithSingleMethod(session, context, THIS_PACKAGE, "AppClass4", "main", false)

        noAppClassTest_testMain = addClassWithSingleMethod(session, context, THIS_PACKAGE, "NoAppClassTest", "testMain", true)
        appClass2Test_testMain = addClassWithSingleMethod(session, context, THIS_PACKAGE, "AppClass2Test", "testMain", true)
        appClass23Test_testMain = addClassWithSingleMethod(session, context, THIS_PACKAGE, "AppClass23Test", "testMain", true)
        appClass234Test_testMain = addClassWithSingleMethod(session, context, THIS_PACKAGE, "AppClass234Test", "testMain", true)
        failingTest_testFail = addClassWithSingleMethod(session, context, THIS_PACKAGE, "FailingTest", "testFail", true)

        session.finishAndApply()
        registry.saveAndOverwriteFile()

        recorder = TestUtils.newRecorder(registry)
    }

    void baseTearDown() throws Exception {
        ///CLOVER:OFF
        if (!IOHelper.delete(tmpDir)) {
            throw new RuntimeException(
                "Unable to delete temporary test directory " + tmpDir.getAbsolutePath())
        }
        ///CLOVER:ON
    }

    protected FullMethodInfo addClassWithSingleMethod(InstrumentationSessionImpl session, ContextSet context, String pkg, String clazzName, String methodName, boolean isTest) {
        return TestUtils.addClassWithSingleMethod(
            session,
            context,
            pkg,
            System.currentTimeMillis(),
            fileSize++,
            clazzName,
            pkg + "." + clazzName + "." + methodName,
            isTest)
    }

    protected void runAppClass234Test_testMain() {
        TestUtils.runTestMethod(
            recorder,
            THIS_PACKAGE + ".AppClass234Test",
            testID,
            appClass234Test_testMain,
            [
                appClass2_main,
                appClass3_main,
                appClass4_main
            ] as FullMethodInfo[])
    }

    protected void runAppClass234Test_testMain(long start, long end) {
        TestUtils.runTestMethod(
            recorder,
            THIS_PACKAGE + ".AppClass234Test",
            testID,
            appClass234Test_testMain,
            [
                appClass2_main,
                appClass3_main,
                appClass4_main
            ] as FullMethodInfo[],
            start,
            end)
    }

    protected void runAppClass23Test_testMain() {
        TestUtils.runTestMethod(
            recorder,
            THIS_PACKAGE + ".AppClass23Test",
            testID,
            appClass23Test_testMain,
            [
                appClass2_main,
                appClass3_main
            ] as FullMethodInfo[])
    }

    protected void runAppClass23Test_testMain(long start, long end) {
        TestUtils.runTestMethod(
            recorder,
            THIS_PACKAGE + ".AppClass23Test",
            testID,
            appClass23Test_testMain,
            [
                appClass2_main,
                appClass3_main
            ] as FullMethodInfo[],
            start,
            end)
    }

    protected void runAppClass2Test_testMain() {
        TestUtils.runTestMethod(
            recorder,
            THIS_PACKAGE + ".AppClass2Test",
            testID,
            appClass2Test_testMain,
            [
                appClass2_main
            ] as FullMethodInfo[])
    }

    protected void runAppClass2Test_testMain(long start, long end) {
        TestUtils.runTestMethod(
            recorder,
            THIS_PACKAGE + ".AppClass2Test",
            testID,
            appClass2Test_testMain,
            [
                appClass2_main
            ] as FullMethodInfo[], start, end)
    }

    protected void runFailingTest_testFail() {
        final long now = System.currentTimeMillis()
        TestUtils.runTestMethod(
            recorder,
            THIS_PACKAGE + ".FailingTest",
            testID,
            failingTest_testFail,
            [
                appClass2_main
            ] as FullMethodInfo[], now, now + 1, new ErrorInfo("Error message", "Error stack trace"))
    }

    protected void runNoAppClassTest_testMain() {
        TestUtils.runTestMethod(recorder, THIS_PACKAGE + ".NoAppClassTest", testID, noAppClassTest_testMain, new FullMethodInfo[0])
    }

    protected void runNoAppClassTest_testMain(long start, long end) {
        TestUtils.runTestMethod(recorder, THIS_PACKAGE + ".NoAppClassTest", testID, noAppClassTest_testMain, new FullMethodInfo[0], start, end)
    }
}
