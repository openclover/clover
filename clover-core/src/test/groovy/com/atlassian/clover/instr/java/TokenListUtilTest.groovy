package com.atlassian.clover.instr.java

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.openclover.util.Maps.newHashMap

public class TokenListUtilTest {

    @Test
    public void testJavadocTagsAndValues() {
        Map tags = newHashMap()

        TokenListUtil.getJDocTagsOnComment(tags, "")
        assertEquals(tags.size(), 0)
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(tags, "    ")
        assertEquals(tags.size(), 0)
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(tags, "\n\n\n\n\n")
        assertEquals(tags.size(), 0)
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(tags, "/** */")
        assertEquals(tags.size(), 0)
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(tags, "/**\n*\n*/")
        assertEquals(tags.size(), 0)
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(tags, "/**")
        assertEquals(tags.size(), 0)
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(tags, "*/")
        assertEquals(tags.size(), 0)
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(tags, "/** */")
        assertEquals(tags.size(), 0)
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(tags, "/**/")
        assertEquals(tags.size(), 0)
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(tags, "@woble!")
        assertEquals(tags.size(), 1)
        assertTrue(tags.containsKey("woble!"))
        assertEquals(((List)tags.get("woble!")).size(), 1)
        assertEquals(((List)tags.get("woble!")).get(0), "")
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(tags, "/** @foo */")
        assertEquals(tags.size(), 1)
        assertTrue(tags.containsKey("foo"))
        assertEquals(((List)tags.get("foo")).size(), 1)
        assertEquals(((List)tags.get("foo")).get(0), "")
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(tags, "/** @foo bar */")
        assertEquals(tags.size(), 1)
        assertTrue(tags.containsKey("foo"))
        assertEquals(((List)tags.get("foo")).size(), 1)
        assertEquals(((List)tags.get("foo")).get(0), "bar")
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(tags, "/** @foo \n* bar */")
        assertEquals(tags.size(), 1)
        assertTrue(tags.containsKey("foo"))
        assertEquals(((List)tags.get("foo")).size(), 1)
        assertEquals(((List)tags.get("foo")).get(0), "bar")
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(tags, "/** @foo a\n* bar\n* baz */")
        assertEquals(tags.size(), 1)
        assertTrue(tags.containsKey("foo"))
        assertEquals(((List)tags.get("foo")).size(), 1)
        assertEquals(((List)tags.get("foo")).get(0), "a bar baz")
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(tags, "/** @foo def \n* @bar abc*/")
        assertEquals(tags.size(), 2)
        assertTrue(tags.containsKey("foo"))
        assertTrue(tags.containsKey("bar"))
        assertEquals(((List)tags.get("foo")).size(), 1)
        assertEquals(((List)tags.get("foo")).get(0), "def")
        assertEquals(((List)tags.get("bar")).size(), 1)
        assertEquals(((List)tags.get("bar")).get(0), "abc")
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(tags, "/** @foo def \n* @bar abc\n*/")
        assertEquals(tags.size(), 2)
        assertTrue(tags.containsKey("foo"))
        assertTrue(tags.containsKey("bar"))
        assertEquals(((List)tags.get("foo")).size(), 1)
        assertEquals(((List)tags.get("foo")).get(0), "def")
        assertEquals(((List)tags.get("bar")).size(), 1)
        assertEquals(((List)tags.get("bar")).get(0), "abc")
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(tags, "/*** blah blah\n@foo def \n    *     @bar abc\n* blah2 blah2=\"123*\"**/")
        assertEquals(tags.size(), 2)
        assertTrue(tags.containsKey("foo"))
        assertTrue(tags.containsKey("bar"))
        assertEquals(((List)tags.get("foo")).size(), 1)
        assertEquals(((List)tags.get("foo")).get(0), "def")
        assertEquals(((List)tags.get("bar")).size(), 1)
        assertEquals(((List)tags.get("bar")).get(0), "abc blah2 blah2=\"123*\"")
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(
            tags,
            " /**\n" +
            " * @testng.test groups = \"a b\"\n" +
            " *              dependsOnMethods = \"m1\"\n" +
            " */")
        assertEquals(tags.size(), 1)
        assertTrue(tags.containsKey("testng.test"))
        assertEquals(((List)tags.get("testng.test")).size(), 1)
        assertEquals(((List)tags.get("testng.test")).get(0), "groups = \"a b\"              dependsOnMethods = \"m1\"")
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(
            tags,
            "/**\n" +
            " * @testng.before-method alwaysRun = \"true\"\n" +
            " */")
        assertEquals(tags.size(), 1)
        assertTrue(tags.containsKey("testng.before-method"))
        assertEquals(((List)tags.get("testng.before-method")).size(), 1)
        assertEquals(((List)tags.get("testng.before-method")).get(0), "alwaysRun = \"true\"")
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(
            tags,
            "/**\n" +
            " * @testng.expected-exceptions\n" +
            " * value = \"java.lang.NullPointerException java.lang.NumberFormatException\"\n" +
            " */")
        assertEquals(tags.size(), 1)
        assertTrue(tags.containsKey("testng.expected-exceptions"))
        assertEquals(((List)tags.get("testng.expected-exceptions")).size(), 1)
        assertEquals(((List)tags.get("testng.expected-exceptions")).get(0), "value = \"java.lang.NullPointerException java.lang.NumberFormatException\"")
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(
            tags,
            "/**\n" +
            " * @testng.parameters value = \"first-name last-name\"\n" +
            " */")
        assertEquals(tags.size(), 1)
        assertTrue(tags.containsKey("testng.parameters"))
        assertEquals(((List)tags.get("testng.parameters")).size(), 1)
        assertEquals(((List)tags.get("testng.parameters")).get(0), "value = \"first-name last-name\"")
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(
            tags,
            "/**\n" +
            " * @testng.factory\n" +
            " * @testng.parameters value = \"first-name last-name\"\n" +
            " */")
        assertEquals(tags.size(), 2)
        assertTrue(tags.containsKey("testng.factory"))
        assertTrue(tags.containsKey("testng.parameters"))
        assertEquals(((List)tags.get("testng.factory")).size(), 1)
        assertEquals(((List)tags.get("testng.factory")).get(0), "")
        assertEquals(((List)tags.get("testng.parameters")).size(), 1)
        assertEquals(((List)tags.get("testng.parameters")).get(0), "value = \"first-name last-name\"")
        tags.clear()

        TokenListUtil.getJDocTagsOnComment(
            tags,
            "/**\n" +
            " * @testng.data-provider name=\"test1\"\n" +
            " */")
        assertEquals(tags.size(), 1)
        assertTrue(tags.containsKey("testng.data-provider"))
        assertEquals(((List)tags.get("testng.data-provider")).size(), 1)
        assertEquals(((List)tags.get("testng.data-provider")).get(0), "name=\"test1\"")
        tags.clear()
    }

    @Test
    public void testIgnoredJavadocTagsAndValues() {
        Map tags = newHashMap()
        TokenListUtil.getJDocTagsOnComment(tags, "/** @param baz\n * @deprecated foo\n * @throws Exception\n * @see me\n * @serialField ABC\n * @serialData DATA\n * @author abc@def.ghi\n * @since 2001\n * @version 1\n * @exception Exception\n */")
        assertEquals(tags.size(), 0)
        tags.clear()
    }
}
