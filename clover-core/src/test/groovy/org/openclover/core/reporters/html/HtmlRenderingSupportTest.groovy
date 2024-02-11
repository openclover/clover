package org.openclover.core.reporters.html

import clover.org.apache.commons.lang3.StringUtils
import com.atlassian.clover.registry.entities.FullClassInfo
import com.atlassian.clover.registry.entities.TestCaseInfo
import com.atlassian.clover.reporters.html.HtmlRenderingSupportImpl
import org.openclover.util.Lists
import com.atlassian.clover.reporters.Format
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class HtmlRenderingSupportTest {

    HtmlRenderingSupportImpl support

    @Before
    void setUp() throws Exception {
        support = new HtmlRenderingSupportImpl(Format.DEFAULT_HTML, true)
    }

    @Test
    void testPackageRelPath() {
        assertEquals("../../../a/b/", support.getPackageRelPath("com.cenqua.a.b", "com.cenqua.c.d.e"))
        assertEquals("../../../../../", support.getPackageRelPath("", "com.cenqua.c.d.e"))
        assertEquals("com/cenqua/a/b/", support.getPackageRelPath("com.cenqua.a.b", ""))
    }

    @Test
    void testConstrain() {
        assertEquals(10, support.constrain(0f, 10, 40))
        assertEquals(40, support.constrain(1f, 10, 40))
        assertEquals(25, support.constrain(0.5f, 10, 40))
        assertEquals(37, support.constrain(0.9f, 10, 40))
    }

    @Test
    void testBlankNonAlpha() {
        assertEquals("Foo", HtmlRenderingSupportImpl.blankNonAlpha("Foo"))
        assertEquals("Foo_Bar", HtmlRenderingSupportImpl.blankNonAlpha("Foo.Bar"))
        assertEquals("Foo_Bar_Baz_abc__", HtmlRenderingSupportImpl.blankNonAlpha('Foo.Bar$Baz.abc()'))
    }

    @Test
    void testJoinWithDots() {
        assertEquals("", support.joinWithDots("", ""));
        assertEquals("abc", support.joinWithDots("abc", ""));
        assertEquals("def", support.joinWithDots("", "def"));
        assertEquals("abc.def", support.joinWithDots("abc", "def"));
    }

    @Test
    void testListAddLast() {
        ArrayList<String> list = Lists.newArrayList()
        support.listAddKeyAsLast(list, "abc")
        assertTrue(list.size() == 1)
        assertEquals("abc", list.get(0))

        support.listAddKeyAsLast(list, "def")
        assertTrue(list.size() == 2)
        assertEquals("abc", list.get(0))
        assertEquals("abc.def", list.get(1))
    }

    @Test
    void testListGetLast() {
        ArrayList<String> listZero = Lists.newArrayList()
        assertNull(support.listGetLast(listZero))

        ArrayList<String> listOne = Lists.newArrayList()
        listOne.add("abc")
        assertEquals("abc", support.listGetLast(listOne))

        listOne.add("def")
        assertEquals("def", support.listGetLast(listOne))
    }

    @Test
    void testListRemoveLast() {
        ArrayList<String> listTwo = Lists.newArrayList()
        listTwo.add("abc")
        listTwo.add("def")

        support.listRemoveLast(listTwo)
        assertEquals(1, listTwo.size())
        support.listRemoveLast(listTwo)
        assertEquals(0, listTwo.size())
        support.listRemoveLast(listTwo)
        assertEquals(0, listTwo.size())
    }

    @Test
    void testShortenName() {
        assertEquals("Should not shorten name within the limit",
                "four",
                HtmlRenderingSupportImpl.shortenName("four", 4))

        assertEquals("Should not shorten name within the limit",
                "my test name",
                HtmlRenderingSupportImpl.shortenName("my test name", 20))

        assertEquals("Should append test name hash if too long",
                "very long testsc8fi0",
                HtmlRenderingSupportImpl.shortenName("very long test name exceeding limit", 20))

        assertEquals("Should return only hash",
                "llo5u7",
                HtmlRenderingSupportImpl.shortenName("even hash cannot fit in the limit", 6))

        assertEquals("Should even truncate hash if it doesn't fit",
                "llo",
                HtmlRenderingSupportImpl.shortenName("even hash cannot fit in the limit", 3))

        assertEquals("Should handle empty strings",
                "",
                HtmlRenderingSupportImpl.shortenName("", 4))

        assertEquals("Should handle null",
                null,
                HtmlRenderingSupportImpl.shortenName(null, 4))

        assertEquals("Should return an empty string for negative limit (case when class name itself is very long)",
                "",
                HtmlRenderingSupportImpl.shortenName("abc", -1))
    }

    @Test
    void testGetTestFileNameTruncatesClassName() {
        // long class name and truncated
        // no test name at all
        FullClassInfo classInfo = mock(FullClassInfo.class)
        when(classInfo.getName()).thenReturn("VeryLongClass" + StringUtils.repeat("X", 300))

        TestCaseInfo tci = mock(TestCaseInfo.class)
        when(tci.getId()).thenReturn(12345)
        when(tci.getRuntimeType()).thenReturn(classInfo)
        when(tci.getTestName()).thenReturn("my test name")

        assertFileLengthLimit(support.getTestFileName(tci))
        assertEquals(
                "VeryLongClass" + StringUtils.repeat("X", 225) + "64qmjq__9ix.html",
                support.getTestFileName(tci).toString())
    }

    @Test
    void testGetTestFileNameTruncatesTestName() {
        // long class name but not truncated
        // long test name, truncated
        String className = "VeryLongClass" + StringUtils.repeat("X", 200)
        FullClassInfo classInfo = mock(FullClassInfo.class)
        when(classInfo.getName()).thenReturn(className)

        TestCaseInfo tci = mock(TestCaseInfo.class)
        when(tci.getId()).thenReturn(12345)
        when(tci.getRuntimeType()).thenReturn(classInfo)
        when(tci.getTestName()).thenReturn("very long test name" + StringUtils.repeat("y", 100))

        assertFileLengthLimit(support.getTestFileName(tci))
        assertEquals(
                className + "_" + "very_long_test_name" + StringUtils.repeat("y", 6) + "k758xq_9ix.html",
                support.getTestFileName(tci).toString())
    }

    private static assertFileLengthLimit(StringBuffer fileName) {
        assertTrue(fileName.length() < 255)
    }
}
