package com.atlassian.clover.reporters.html

import org.openclover.util.Lists
import com.atlassian.clover.reporters.Format
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue

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
}
