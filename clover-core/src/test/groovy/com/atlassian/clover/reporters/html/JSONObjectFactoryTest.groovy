package com.atlassian.clover.reporters.html

import clover.com.google.common.collect.Lists
import clover.com.google.common.collect.Maps
import com.atlassian.clover.context.ContextSet
import com.atlassian.clover.registry.entities.BasicMethodInfo
import com.atlassian.clover.registry.entities.FullClassInfo
import com.atlassian.clover.registry.entities.FullFileInfo
import com.atlassian.clover.registry.FixedSourceRegion
import com.atlassian.clover.registry.entities.FullMethodInfo
import com.atlassian.clover.registry.entities.MethodSignature
import com.atlassian.clover.registry.entities.Modifiers
import com.atlassian.clover.registry.metrics.HasMetricsTestFixture
import com.atlassian.clover.registry.entities.TestCaseInfo
import com.atlassian.clover.spi.reporters.html.source.LineRenderInfo
import com.atlassian.clover.reporters.json.JSONException
import com.atlassian.clover.reporters.json.JSONObject
import junit.framework.TestCase

class JSONObjectFactoryTest extends TestCase {

    private final int indentFactor = 2

    private JSONObject getExpectedTargets() throws JSONException {
        JSONObject json = new JSONObject()

        List methodsList1 = Lists.newArrayList()
        methodsList1.add(Collections.singletonMap("sl", new Integer(0)))

        List statementsList1 = Lists.newArrayList()
        statementsList1.add(Collections.singletonMap("sl", new Integer(1)))

        Map classMap1 = Maps.newHashMap()
        classMap1.put("statements", statementsList1)
        classMap1.put("pass", Boolean.FALSE)
        classMap1.put("methods", methodsList1)
        classMap1.put("name", "test1-runtime")

        List methodsList2 = Lists.newArrayList()
        methodsList2.add(Collections.singletonMap("sl", new Integer(0)))
        methodsList2.add(Collections.singletonMap("sl", new Integer(1)))

        Map classMap2 = Maps.newHashMap()
        classMap2.put("statements", Lists.newArrayList())
        classMap2.put("pass", Boolean.FALSE)
        classMap2.put("methods", methodsList2)
        classMap2.put("name", "test0-runtime")

        json.put("test_0", classMap2)
        json.put("test_1", classMap1)

        return json
    }


    private JSONObject getExpectedEmptyElementsTargets() throws JSONException {
        JSONObject json = new JSONObject()

        List methodsList1 = Lists.newArrayList()
        methodsList1.add(Collections.singletonMap("sl", new Integer(0)))

        List statementsList1 = Lists.newArrayList()

        Map classMap1 = Maps.newHashMap()
        classMap1.put("statements", statementsList1)
        classMap1.put("pass", Boolean.FALSE)
        classMap1.put("methods", methodsList1)
        classMap1.put("name", "test0-runtime")

        json.put("test_0", classMap1)

        return json
    }

    private List getExpectedSrcLines() {
        List list = Lists.newArrayList()
        list.add(Lists.newArrayList())
        list.add(Lists.newArrayList())

        List listInner1 = Lists.newArrayList()
        listInner1.add(new Integer(0))
        list.add(listInner1)

        List listInner2 = Lists.newArrayList()
        listInner2.add(new Integer(0))
        listInner2.add(new Integer(1))
        listInner2.add(new Integer(2))
        list.add(listInner2)

        return list
    }

    private JSONObject getExpectedPageData1() throws JSONException {
        JSONObject json = new JSONObject()
        json.put("classes", Lists.newArrayList())
        return json
    }

    private JSONObject getExpectedPageData2() throws JSONException {
        JSONObject json = new JSONObject()

        List classesList = Lists.newArrayList()

        Map classMap1 = Maps.newHashMap()
        classMap1.put("sl", new Integer(8))
        classMap1.put("el", new Integer(12))
        classMap1.put("name", "TestClass2")
        classMap1.put("id", new Integer(1))
        classMap1.put("methods", Lists.newArrayList())

        List methodsList2 = Lists.newArrayList()
        Map methodsMap2 = Maps.newHashMap()
        methodsMap2.put("sl", new Integer(3))
        methodsMap2.put("el", new Integer(3))
        methodsMap2.put("sc", new Integer(1))
        methodsList2.add(methodsMap2)

        Map classMap2 = Maps.newHashMap()
        classMap2.put("sl", new Integer(2))
        classMap2.put("el", new Integer(6))
        classMap2.put("name", "TestClass1")
        classMap2.put("id", new Integer(0))
        classMap2.put("methods", methodsList2)

        classesList.add(classMap2)
        classesList.add(classMap1)
        json.put("classes", classesList)

        return json
    }

    void testGetJSONPageData() throws IOException, JSONException {
        FullFileInfo finfo = new FullFileInfo(null, new File("TestFile"), null, 0, 10, 10, 0, 10, 0, 0)
        JSONObject json = JSONObjectFactory.getJSONPageData(finfo)
        assertEquals(getExpectedPageData1().toString(indentFactor), json.toString(indentFactor))

        FullClassInfo cinfo1 = new FullClassInfo(null, finfo, 0, "TestClass1",
                new FixedSourceRegion(2, 4, 6, 4), new Modifiers(),
                false, false, false)
        FullClassInfo cinfo2 = new FullClassInfo(null, finfo, 1, "TestClass2",
                new FixedSourceRegion(8, 4, 12, 4), new Modifiers(),
                false, false, false)
        HasMetricsTestFixture testFixture = new HasMetricsTestFixture(getName())
        testFixture.newMethod(cinfo1, "testMethod", 3)

        finfo.addClass(cinfo1)
        finfo.addClass(cinfo2)
        json = JSONObjectFactory.getJSONPageData(finfo)
        assertEquals(getExpectedPageData2().toString(indentFactor), json.toString(indentFactor))
    }

    void testGetJSONSrcFileLines() {
        LineRenderInfo[] renderInfo = new LineRenderInfo[3]

        List testList = Lists.newArrayList()
        renderInfo[0] = new LineRenderInfo()
        renderInfo[0].setTestHits(Lists.newArrayList(testList))

        testList.add(new TestCaseInfo(new Integer(0), null, fixtureMethod("test0"), "test0-runtime"))
        renderInfo[1] = new LineRenderInfo()
        renderInfo[1].setTestHits(Lists.newArrayList(testList))

        testList.add(new TestCaseInfo(new Integer(1), null, fixtureMethod("test1"), "test1-runtime"))
        testList.add(new TestCaseInfo(new Integer(2), null, fixtureMethod("test2"), "test2-runtime"))
        renderInfo[2] = new LineRenderInfo()
        renderInfo[2].setTestHits(Lists.newArrayList(testList))

        List json = JSONObjectFactory.getJSONSrcFileLines(renderInfo, "test")
        assertEquals(getExpectedSrcLines().toString(), json.toString())
    }

    void testGetJSONTestTargets() throws JSONException {
        TestCaseInfo testcase0 = new TestCaseInfo(new Integer(0), null, fixtureMethod("test0"), "test0-runtime")
        TestCaseInfo testcase1 = new TestCaseInfo(new Integer(1), null, fixtureMethod("test1"), "test1-runtime")

        BitSet mbs0 = new BitSet(2)
        mbs0.set(0, true)
        mbs0.set(1, true)

        BitSet mbs1 = new BitSet(2)
        mbs1.set(0, true)
        mbs1.set(1, false)

        Map targetMethods = Maps.newHashMap()
        targetMethods.put(testcase0, mbs0)
        targetMethods.put(testcase1, mbs1)

        BitSet ebs0 = new BitSet(2)
        ebs0.set(0, false)
        ebs0.set(1, false)

        BitSet ebs1 = new BitSet(2)
        ebs1.set(0, false)
        ebs1.set(1, true)

        Map targetElements = Maps.newHashMap()
        targetElements.put(testcase0, ebs0)
        targetElements.put(testcase1, ebs1)

        JSONObject json = JSONObjectFactory.getJSONTestTargets(targetMethods, targetElements)
        assertEquals(getExpectedTargets().toString(indentFactor), json.toString(indentFactor))
    }

    void testGetJSONTestTargetsWithEmptyElements() throws JSONException {
        TestCaseInfo testcase0 = new TestCaseInfo(new Integer(0), null, fixtureMethod("test0"), "test0-runtime")
        Map targetMethods = Maps.newHashMap()
        Map targetElements = Maps.newHashMap()

        BitSet mbs0 = new BitSet(1)
        mbs0.set(0, true)
        targetMethods.put(testcase0, mbs0)

        //no statements

        JSONObject json = JSONObjectFactory.getJSONTestTargets(targetMethods, targetElements)
        assertEquals(getExpectedEmptyElementsTargets().toString(indentFactor), json.toString(indentFactor))
    }

    private FullMethodInfo fixtureMethod(String name) {
        FullFileInfo parentFile = new FullFileInfo(null, new File("FakeFile.java"), null, 0, 0, 0, 0, 0, 0, 0)
        FullClassInfo parentClass = new FullClassInfo(null, parentFile, 0, null, new FixedSourceRegion(0,0), new Modifiers(), false, false, false)
        return new FullMethodInfo(parentClass,
                new ContextSet(),
                new BasicMethodInfo(new FixedSourceRegion(0, 0), 0, 0, new MethodSignature(name), true, null, false) )
    }
    
}
