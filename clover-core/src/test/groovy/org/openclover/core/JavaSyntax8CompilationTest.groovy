package org.openclover.core

import org.junit.Before
import org.junit.Test
import org.openclover.core.api.registry.Annotation
import org.openclover.core.api.registry.AnnotationValue
import org.openclover.core.api.registry.ClassInfo
import org.openclover.core.api.registry.MethodInfo
import org.openclover.core.registry.entities.FullProjectInfo
import org.openclover.core.registry.entities.StringifiedAnnotationValue
import org.openclover.core.util.JavaEnvUtils
import org_openclover_runtime.CoverageRecorder

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.CoreMatchers.hasItems
import static org.hamcrest.MatcherAssert.assertThat
import static org.junit.Assert.assertEquals
import static org.junit.Assume.assumeThat
import static org.junit.Assume.assumeTrue
import static org.openclover.core.util.Lists.newArrayList

/**
 * The purpose of this test is to
 * a) make sure the code compiles under a JDK1.8
 * b) make sure that when that code is instrumented, it still compiles
 */
class JavaSyntax8CompilationTest extends JavaSyntaxCompilationTestBase {

    protected File srcDir

    @Before
    void setUp() throws Exception {
        setUpProject()
        srcDir = new File(mTestcasesSrcDir, "javasyntax1.8")
        resetAntOutput()
    }

    @Test
    void testAnnotationsOnJavaTypes() {
        final String fileName = "AnnotationsOnJavaTypes.java"
        instrumentSourceFile(new File(srcDir, fileName), JavaEnvUtils.JAVA_8)

        // check that annotations put in various places does not interfere with Clover's instrumentation
// TODO IT SEEMS THAT JDK1.8 M7 DOES NOT SUPPORT THESE ANNOTATIONS YET
//            assertFileMatches(fileName,
//                    ".*int size\\(\\) @Readonly \\{.*__CLR.*", false)
//            assertFileMatches(fileName,
//                    ".*__CLR.*inc.*myString = \\(@NonNull String\\)myObject.*", false)
//            assertFileMatches(fileName,
//                    ".*__CLR.*inc.*myString instanceof @NonNull String.*", false)
//            assertFileMatches(fileName,
//                    ".*__CLR.*inc.*new @NonEmpty @Readonly List.*", false)
//            assertFileMatches(fileName,
//                    ".*__CLR.*inc.*void monitorTemperature\\(\\) throws @Critical TemperatureException \\{.*__CLR.*", false)
//            assertFileMatches(fileName,
//                    ".*__CLR.*inc.*.*", false)
    }

    @Test
    void testLambdaAndMethodReferences() throws IOException {
        final String fileName = "LambdaAndMethodReferences.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)
        executeMainClasses([ fileName.replace(".java", "") ] as String[])

        assertFileMatches(fileName,
                R_INC + "transform\\(" + R_LAMBDA_INC_LEFT + "myString::toLowerCase" + R_LAMBDA_INC_RIGHT + "\\)", false)
        assertFileMatches(fileName,
                ".*__CLR.*myStringList\\.forEach\\(" + R_LAMBDA_INC_LEFT + "String::toLowerCase" + R_LAMBDA_INC_RIGHT + "\\)", false)
        assertFileMatches(fileName,
                ".*__CLR.*Runnable callGc = " + R_LAMBDA_INC_LEFT + "System::gc" + R_LAMBDA_INC_RIGHT + ";", false)
        assertFileMatches(fileName,
                ".*__CLR.*Arrays\\.sort\\(myArray, " + R_LAMBDA_INC_LEFT + "Integer::compare" + R_LAMBDA_INC_RIGHT + "\\);", false)
    }

    @Test
    void testLambdaAndTest() {
        final String fileName = "LambdaAndTest.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)

        assertFileMatches(fileName,
                "Runnable r = ${R_LAMBDA_INC_LEFT}System::currentTimeMillis${R_LAMBDA_INC_RIGHT}".toString(), false)
        assertFileMatches(fileName,
                "static class __CLR.*public static ${CoverageRecorder.class.name}".toString(), false)
    }

    @Test
    void testLambdaAndCloverOff() throws Exception {
        final String fileName = "LambdaAndCloverOff.java"
        final String className = fileName.replace(".java", "")
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)
        executeMainClasses([ className ] as String[])

        // fields
        assertNoStatement(className, 13) // aroundFieldExpr
        assertStatementCoverage(className, 14, 0) // aroundFieldBlock

        // testCloverOffAroundLambda
        assertMethodCoverage(className, 20, 1)
        assertStatementCoverage(className, 22, [ 0 ] as int[]) // aroundLamExpr - 1 statement, as no lambdaInc here
        assertStatementCoverage(className, 23, [ 0, 0 ] as int[]) // aroundLamBlock - 2 statements - assignment + 'return null'

        // testCloverOffAroundMethod
        assertMethodCoverage(className, 31, 0)
        assertStatementCoverage(className, 32, 0) // aroundMethodExpr - 1 statement, as no lambdaInc here
        assertStatementCoverage(className, 33, [ 0, 0 ] as int[]) // aroundMethodBlock - 2 statements - assignment + return null

        // testCloverOffInsideLambda
        assertMethodCoverage(className, 40, 1)
        assertStatementCoverage(className, 41, 1) // insideLamExpr
        assertNoStatement(className, 43)

        assertStatementCoverage(className, 48, 1) // insideLamBlock
        assertNoStatement(className, 50)

        // testCloverOffIntersectingLambda
        assertStatementCoverage(className, 61, 1) // intersectLamExpr
        assertNoStatement(className, 71)
        assertNoStatement(className, 78) // intersectLamBlock
        assertStatementCoverage(className, 81, 1)
        assertStatementCoverage(className, 86, 1)

        assertMethodCoverage(className, 91, 1) // main
    }

    @Test
    void testLambdaAndConstructorReferences() throws IOException {
        final String fileName = "LambdaAndConstructorReferences.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)
        executeMainClasses([ fileName.replace(".java", "") ] as String[])

        // testObjectTypeReference
        assertFileMatches(fileName,
                ".*__CLR.*Produce<String> createString = " + R_LAMBDA_INC_LEFT + "String::new" + R_LAMBDA_INC_RIGHT + ";", false)

        // testArrayReference
        assertFileMatches(fileName,
                ".*__CLR.*ProduceString createStringArray = " + R_LAMBDA_INC_LEFT + "String\\[\\]::new" + R_LAMBDA_INC_RIGHT + ";", false)

        // testRawTypeReference
        assertFileMatches(fileName,
                ".*__CLR.*ProduceRaw createRawList = " + R_LAMBDA_INC_LEFT + "ArrayList::new" + R_LAMBDA_INC_RIGHT + ";", false)

        // testGenericTypeReference
        assertFileMatches(fileName,
                ".*__CLR.*Produce<ArrayList<String>> createStringList = " + R_LAMBDA_INC_LEFT + "ArrayList<String>::new" + R_LAMBDA_INC_RIGHT + ";", false)
        assertFileMatches(fileName,
                ".*__CLR.*Produce<ArrayList<String>> createStringList2 = " + R_LAMBDA_INC_LEFT + "ArrayList<String>::<String>new" + R_LAMBDA_INC_RIGHT + ";", false)
        assertFileMatches(fileName,
                ".*__CLR.*Produce<One\\.Two<Integer>> oneTwo = " + R_LAMBDA_INC_LEFT + "One\\.Two<Integer>::new" + R_LAMBDA_INC_RIGHT + ";", false)
        assertFileMatches(fileName,
                ".*__CLR.*Produce<String> oneThreeHi = " + R_LAMBDA_INC_LEFT + "ones\\[3 - 2 - 1\\]\\.three::<Integer>hi" + R_LAMBDA_INC_RIGHT + ";", false)

        // testReferenceWithTypeCast
        assertFileMatches(fileName,
                ".*__CLR.*Object oo = \\(Produce<String>\\)" + R_LAMBDA_INC_LEFT + "\\(Produce<String>\\)String::new" + R_LAMBDA_INC_RIGHT + ";", false)
        assertFileMatches(fileName,
                ".*__CLR.*Produce<One\\.Two<Integer>> oneTwoCast = \\(Produce<One\\.Two<Integer>> & Serializable\\)" +
                        R_LAMBDA_INC_LEFT + "\\(Produce<One\\.Two<Integer>> & Serializable\\)One\\.Two<Integer>::new" + R_LAMBDA_INC_RIGHT + ";", false)
    }

    @Test
    void testLambdaAndGenerics() throws IOException {
        final String fileName = "LambdaAndGenerics.java"

        File srcFile = new File(srcDir, fileName)
        resetAntOutput()
        instrumentSourceFile(srcFile, JavaEnvUtils.JAVA_8, [
                "--instrlambda", "block" // test: don't instrument expression lambdas
        ] as String[])
        compileSources(mGenSrcDir, [ fileName ] as String[], JavaEnvUtils.JAVA_8)

        // "foo(e -> true)" shall not be instrumented
        assertFileContains(fileName, "foo(e -> true)", false)
        // "foo(e -> { return true; })" shall use block lambda emitter
        assertFileMatches(fileName, "foo\\(e -> \\{" + R_INC + " " + R_INC + "return true; \\}\\)", false)
        // "goo(e -> false)" shall not be instrumented
        assertFileContains(fileName, "goo(e -> false)", false)

        executeMainClasses([ fileName.replace(".java", "") ] as String[])

        assertExecOutputContains("DoesNotFail predicate: true", false)
        assertExecOutputContains("Fails predicate: false", false)
    }

    @Test
    void testLambdaArgumentAndReturnTypes() throws IOException {
        final String fileName = "LambdaArgumentAndReturnTypes.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)
        executeMainClasses([ fileName.replace(".java", "") ] as String[])

        assertFileMatches(fileName,
                "Join<Integer> sumIntegers = " + R_LAMBDA_INC_LEFT + "\\(Integer x, Integer y\\) -> x \\+ y" + R_LAMBDA_INC_RIGHT + ";",
                false)
        assertFileMatches(fileName,
                "Join<Integer> sumGuessWhat = " + R_LAMBDA_INC_LEFT + "\\(x, y\\) -> x \\+ y" + R_LAMBDA_INC_RIGHT + ";",
                false)
        assertFileMatches(fileName,
                "Execute printHello = " + R_LAMBDA_INC_LEFT + "\\(\\) -> System\\.out\\.println\\(\"Hello World\"\\)" + R_LAMBDA_INC_RIGHT + ";",
                false)
        assertFileMatches(fileName,
                "Produce<String> produce = " + R_LAMBDA_INC_LEFT + "\\(\\) -> \"created\"" + R_LAMBDA_INC_RIGHT + ";",
                false)
        assertFileMatches(fileName,
                "Consume<String> printString = " + R_LAMBDA_INC_LEFT + "\\(String s\\) -> System\\.out\\.println\\(s\\)" + R_LAMBDA_INC_RIGHT + ";",
                false)
        assertFileMatches(fileName,
                "Consume<String> printString = " + R_LAMBDA_INC_LEFT + "\\(s\\) -> System\\.out\\.println\\(s\\)" + R_LAMBDA_INC_RIGHT + ";",
                false)
        assertFileMatches(fileName,
                "Consume<String> printString = " + R_LAMBDA_INC_LEFT + "s -> System\\.out\\.println\\(s\\)" + R_LAMBDA_INC_RIGHT + ";",
                false)
        assertFileMatches(fileName,
                "Map<Double> multiply = " + R_LAMBDA_INC_LEFT + "\\(x\\) -> 2 \\* x" + R_LAMBDA_INC_RIGHT + ";",
                false)
        assertFileMatches(fileName,
                "Map<Double> multiply = " + R_LAMBDA_INC_LEFT + "x -> 3 \\* x" + R_LAMBDA_INC_RIGHT + ";",
                false)
    }

    @Test
    void testLambdaInContexts() throws IOException {
        final String fileName = "LambdaInContexts.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)
        executeMainClasses([ fileName.replace(".java", "") ] as String[])

        // lambdaInVariableDeclarationAndAssignment
        assertFileMatches(fileName,
                ".*Map<Integer> duplicate = " + R_LAMBDA_INC_LEFT + "x -> 2 \\* x" + R_LAMBDA_INC_RIGHT + ";.*",
                false)
        assertFileMatches(fileName,
                ".*final Integer\\[\\] table10 = produceN\\("
                        + R_LAMBDA_INC_LEFT + "Integer\\[\\]::new" + R_LAMBDA_INC_RIGHT
                        + ", 10\\);.*",
                false)
        assertFileMatches(fileName,
                ".*final Integer\\[\\] table100 = produceN\\("
                        + "n -> \\{" + R_INC + " " + R_INC + "return new Integer\\[100 \\* n\\]; }"
                        + ", 1\\);.*",
                false)

        // lambdaInReturnStatement
        assertFileMatches(fileName,
                ".*return " + R_LAMBDA_INC_LEFT + "x -> 2 \\* x" + R_LAMBDA_INC_RIGHT + ";.*",
                false)

        // lambdaAsMethodArgument
        assertFileMatches(fileName,
                ".*print\\( "
                        + "\\(\\) -> \\{" + R_INC + " " + R_INC + "String out = \"\"; " + R_INC + "return out; \\}"
                        + "\\);.*",
                false)
        assertFileMatches(fileName,
                ".*print\\( " + R_LAMBDA_INC_LEFT + "\\(\\) -> 100" + R_LAMBDA_INC_RIGHT + " \\);.*",
                false)
        assertFileMatches(fileName,
                ".*print\\(" + R_LAMBDA_INC_LEFT + "Object::new" + R_LAMBDA_INC_RIGHT + "\\);.*",
                false)

        // lambdaInLambda
        assertFileMatches(fileName,
                ".*Callable<Runnable> call = " + R_LAMBDA_INC_LEFT + "\\(\\) -> \\(\\) -> \\{"
                        + R_INC + " "
                        + R_INC + "System\\.out\\.println\\(\"Callable calls Runnable which calls run\"\\); }"
                        + R_LAMBDA_INC_RIGHT + ";.*",
                false)

        // lambdaInTernaryExpression
        //     Callable<Integer> c1 = flag ? () -> 23 : () -> 42
        // gets statement coverage, e.g.:
        //     __CLR3_2_000hlhsfq5q.R.inc(27);Callable<Integer> c1 = ...
        // gets branch coverage for "? :", e.g.:
        //     c1 = (((flag )&&(__CLR3_2_000hlhsfq5q.R.iget(28)!=0|true))||(__CLR3_2_000hlhsfq5q.R.iget(29)==0&false))?...
        // gets extra stuff for lambdas like:
        //     ? __CLR3_2_000hlhsfq5q.R.lambdaInc(() -> 23, 30) : __CLR3_2_000hlhsfq5q.R.lambdaInc(() -> 42, 31)
        assertFileMatches(fileName,
                R_INC + "Callable<Integer> c1",
                false)
        assertFileMatches(fileName,
                "c1 = \\(\\(\\(flag \\)&&" + R_IGET_TRUE + "\\)\\|\\|" + R_IGET_FALSE + "\\)\\?",
                false)
        assertFileMatches(fileName,
                "\\? " + R_LAMBDA_INC_LEFT + "\\(\\) -> 23" + R_LAMBDA_INC_RIGHT +
                        " : " + R_LAMBDA_INC_LEFT + "\\(\\) -> 42" + R_LAMBDA_INC_RIGHT + ";",
                false)

        // lambdaWithCastExpression
        assertFileMatches(fileName,
                ".*Object o = \\(Runnable\\) "
                        + "\\(\\) -> \\{" + R_INC + " " + R_INC + "System\\.out\\.println\\(\"lambdaWithCastExpression cast a lambda\"\\); }"
                        + ";.*",
                false)

        // lambdaWithCastExpression - lambdaInc with type cast? copy the cast inside lambdaInc
        assertFileMatches(fileName,
                ".*Object oo = \\(Runnable\\) " + R_LAMBDA_INC_LEFT
                        + "\\(Runnable\\)\\(\\) -> System\\.out\\.println\\(\"lambdaWithCastExpression cast a lambda one-liner\"\\)"
                        + R_LAMBDA_INC_RIGHT + ";.*",
                false)

        // lambdaWithCastExpression - lambdaInc with type cast? copy the cast inside lambdaInc
        assertFileMatches(fileName,
                ".*Object oo2 = \\(Runnable & Serializable\\) "
                        + R_LAMBDA_INC_LEFT + "\\(Runnable & Serializable\\)"
                        + "\\(\\) -> System\\.out\\.println\\(\"lambdaWithCastExpression one-liner cast with serializable\"\\)"
                        + R_LAMBDA_INC_RIGHT + ";.*",
                false)

        // lambdaRecursion
        assertFileMatches(fileName,
                ".*static Map<Integer> factorial = "
                        + R_LAMBDA_INC_LEFT + "i -> "                                                   // "lambdaInc(111, i ->"
                        + "\\(\\(\\(i == 0 \\)&&" + R_IGET_TRUE + "\\)\\|\\|" + R_IGET_FALSE + "\\)\\?" // "i == 0 ?" with branch coverage
                        + " 1 : i \\* LambdaInContexts\\.factorial\\.map\\(i - 1\\)"                                         // "1 : i * factorial.map(i - 1)"
                        + R_LAMBDA_INC_RIGHT + ";.*",                                                   // ");"
                false)
    }

    @Test
    void testLambdaOneLinersAndBlocks() throws IOException {
        final String fileName = "LambdaOneLinersAndBlocks.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir,fileName, JavaEnvUtils.JAVA_8)
        executeMainClasses([ fileName.replace(".java", "") ] as String[])

        // lambdaWithOneExpressionReturningVoid
        assertFileMatches(fileName,
                "Execute sayHello = " + R_LAMBDA_INC_LEFT + "\\(\\) -> System\\.out\\.print\\(\"Hello\"\\)" + R_LAMBDA_INC_RIGHT + ";",
                false)

        // lambdaWithOneExpressionReturningInteger
        assertFileMatches(fileName,
                "Produce<Integer> getInt = " + R_LAMBDA_INC_LEFT + "\\(\\) -> 777" + R_LAMBDA_INC_RIGHT + ";",
                false)

        // lambdaWithOneExpressionThrowingException
        assertExecOutputContains("Exception caught: java.lang.RuntimeException", false)

        // lambdaWithBlockReturningVoid
        assertFileMatches(fileName,
                "Execute sayHello = \\(\\) -> \\{" + R_INC + " "        // inc for method entry
                        + R_INC + "System\\.out\\.print\\(\"Hello\"\\); "
                        + R_INC + "System\\.out\\.print\\(\"Hello\"\\); "
                        + R_INC + "System\\.out\\.print\\(\"Hello\"\\); };",
                false)

        // lambdaWithBlockReturningInteger
        assertFileMatches(fileName,
                "Produce<Integer> getInt = \\(\\) -> \\{" + R_INC + " " // inc for method entry
                        + R_INC + "int i = 777; "
                        + R_INC + "i \\+= 777; "
                        + R_INC + "return i; };",
                false)

        // lambdaWithBlockAndNestedBlockReturningInteger
        assertFileMatches(fileName,
                "Produce<Integer> getInt = \\(\\) -> \\{" + R_INC      // inc for method entry
                        + "  \\{ " + R_INC + "int i = 777; } "
                        + R_INC + "return 777; };",
                false)
    }

    /**
     * Test for RepeatingAnnotations.java code sample.

     * @throws Exception
     */
    @Test
    void testRepeatingAnnotations() throws Exception {
        final String fileName = "RepeatingAnnotations.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)

        // find the RepeatingAnnotations.foo()
        final FullProjectInfo model = getModel()
        ClassInfo c = model.findClass("RepeatingAnnotations")
        assertEquals(1, c.getMethods().size())
        MethodInfo m = c.getMethods().get(0)
        assertEquals("foo", m.getSimpleName())

        // foo() has only one kind of annotation - Bit
        assertEquals(1, m.getSignature().getAnnotations().size())
        // but there are three occurrences of Bit
        Collection<? extends Annotation> bitAnnotations = m.getSignature().getAnnotations().get("Bit")
        assertEquals(3, bitAnnotations.size())

        // Annotations:
        //   name=Foo, attributes=( "value" -> "1" )
        //   name=Foo, attributes=( "value" -> "2" )
        //   name=Foo, attributes=( "value" -> "3" )
        Collection<String> defaultAttributeValues = newArrayList()
        for (Annotation bitAnnotation : bitAnnotations) {
            Map<String, ? extends AnnotationValue> attributes = bitAnnotation.getAttributes()
            defaultAttributeValues.add( ((StringifiedAnnotationValue)attributes.get("value")).getValue() )
        }
        assertThat(defaultAttributeValues, hasItems("1"))
        assertThat(defaultAttributeValues, hasItems("2"))
        assertThat(defaultAttributeValues, hasItems("3"))
    }

    @Test
    void testVirtualExtensionMethod() throws Exception {
        final String fileName = "VirtualExtensionMethod.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)

        // interfaces code shall be instrumented, i.e. the default methods
        assertFileMatches(fileName,
                ".*__CLR.*return !hasNext\\(\\);.*",
                false)

        // new 'default' modifier shall be stored in the method's metadata
        final ClassInfo c = getModel().findClass("VirtualExtensionMethod")
        assertEquals(2, c.getMethods().size())
        for (final MethodInfo methodInfo : c.getMethods()) {
            if (methodInfo.getSimpleName() == "isLast") {
                assertEquals("default boolean isLast()", methodInfo.getSignature().getNormalizedSignature())
            } else if (methodInfo.getSimpleName() == "forwardToLast") {
                assertEquals("void forwardToLast()", methodInfo.getSignature().getNormalizedSignature())
            } else if (methodInfo.getSimpleName() == "chooseLast") {
                // note: default comes before other modifiers
                assertEquals("default public void chooseLast(int option)", methodInfo.getSignature().getNormalizedSignature())
            } else {
                fail("unexpected method found: " + methodInfo.getName())
            }
        }
    }

    @Test
    void testVirtualExtensionMethodAndInheritance() throws Exception {
        final String fileName = "VirtualExtensionMethodAndInheritance.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)
        executeMainClasses([ "VirtualExtensionMethodAndInheritance" ] as String[])

        assertMethodCoverage("VirtualExtensionMethodAndInheritance", 6, 0)  // no coverage for "Hello World from A"
        assertMethodCoverage("VirtualExtensionMethodAndInheritance", 12, 1) // 1 hit count for "Hello World from B"
        assertMethodCoverage("VirtualExtensionMethodAndInheritance", 18, 1)
        assertMethodCoverage("VirtualExtensionMethodAndInheritance", 23, 1)
    }

    @Test
    void testLambdaAndStreams() {
        final String fileName = "LambdaAndStreams.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)
    }

    @Test
    void testPackageInfo_Java8() {
        // @XmlAccessorType annotation is deprecated in JDK 9 and not available in JDK 11 or later
        assumeThat(JavaEnvUtils.getJavaVersion(), equalTo("8"))

        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, "pck/package-info.java", JavaEnvUtils.JAVA_8)
    }

    @Test
    void testPackageInfo_Java9AndAbove() {
        // @XmlAccessorType annotation is deprecated in JDK 9 and not available in JDK 11 or later
        assumeTrue(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_9))

        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, "pck2/package-info.java", JavaEnvUtils.JAVA_9)
    }

    @Test
    void testTypeAnnotationInstanceofOperator() {
        final String fileName = "typeannotation/instanceofoperator/TypeAnnotationInstanceofOperator.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)
    }

    @Test
    void testTypeAnnotationThrowsClause() {
        final String fileName = "typeannotation/throwsclause/TypeAnnotationThrowsClause.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)
    }

    @Test
    void testTypeAnnotationInImplements() {
        final String fileName = "typeannotation/implementsclause/TypeAnnotationImplements.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)
    }

    @Test
    void testTypeAnnotationInTypeParameter() {
        final String fileName = "typeannotation/typeparameter/TypeAnnotationTypeParameter.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)
    }

    @Test
    void testTypeAnnotationInFieldDeclarationPostposingArray() {
        final String fileName = "typeannotation/fielddeclarationpostposingarray/TypeAnnotationInFieldDeclarationPostposingArray.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)
    }

    @Test
    void testPrimitiveArrayConstructorReference() {
        final String fileName = "java8/Java8ArrayConstructorReference.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)
    }

    @Test
    void testTypeAnnotationInNewOperatorArray() {
        final String fileName = "typeannotation/newoperatorarray/TypeAnnotationInNewOperatorArray.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)
    }

    @Test
    void testTypeAnnotationInFieldDeclarationArray() {
        final String fileName = "typeannotation/fielddeclarationarray/TypeAnnotationInFieldDeclarationArray.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)
    }

    @Test
    void testTypeAnnotationNewOperator() {
        final String fileName = "typeannotation/newoperator/TypeAnnotationInNewOperator.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)
    }

    @Test
    void testTypeAnnotationInFieldDeclarationGeneric() {
        final String fileName = "typeannotation/fielddeclarationgenerics/TypeAnnotationInFieldDeclarationGenerics.java"
        instrumentAndCompileSourceFile(srcDir, mGenSrcDir, fileName, JavaEnvUtils.JAVA_8)
        assertFileMatches(fileName, R_INC + "System.out.println", false)
    }
}
