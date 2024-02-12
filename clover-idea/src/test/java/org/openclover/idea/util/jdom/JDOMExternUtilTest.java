package org.openclover.idea.util.jdom;

import org.openclover.idea.SampleEnum;
import org.openclover.core.util.collections.Pair;
import org.junit.Test;
import repkg.org.openclover.idea.RpkgOnlyEnum;

import java.awt.Color;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class JDOMExternUtilTest {
    private static final String CYAN_XML = "<highlight class=\"java.awt.Color\">-16711681</highlight>";
    private static final String CYAN_XML_NL = "<highlight class=\"java.awt.Color\">  \n    -16711681\n  </highlight>";


    private static final String NULL_XML = "<highlight class=\"org.openclover.core.util.collections.Pair\" null=\"true\"></highlight>";
    private static final String NULL_NULL_XML = "<highlight class=\"org.openclover.core.util.collections.Pair\"></highlight>";
    private static final String CYAN_NULL_XML = "<highlight class=\"org.openclover.core.util.collections.Pair\">-16711681 \n\t</highlight>";
    private static final String CYAN_RED_XML = "<highlight class=\"org.openclover.core.util.collections.Pair\">-16711681 \n\t-65536</highlight>";

    private static final String HIGHLIGHT_NULL = "<highlight null=\"true\" />";
    private static final String HIGHLIGHT_CLASS_NULL = "<highlight class=\"org.openclover.core.util.collections.Pair\" null=\"true\" />";
    private static final String HIGHLIGHT_CLASS_CYAN = "<highlight class=\"org.openclover.core.util.collections.Pair\">-16711681</highlight>";
    private static final String HIGHLIGHT_CLASS_CYAN_RED = "<highlight class=\"org.openclover.core.util.collections.Pair\">-16711681 -65536</highlight>";

    @Test
    public void testColorWithNewlines() throws Exception {
        Color c = (Color) JDOMExternUtil.readFromString(CYAN_XML);
        assertEquals(Color.CYAN, c);

        c = (Color) JDOMExternUtil.readFromString(CYAN_XML_NL);
        assertEquals(Color.CYAN, c);
    }

    @Test
    public void testReadColorPair() throws Exception {
        // only one color was provided - use the same for both colors
        Pair pairCyanNull = (Pair)JDOMExternUtil.readFromString(CYAN_NULL_XML);
        assertEquals(Color.CYAN, pairCyanNull.first);
        assertEquals(Color.CYAN, pairCyanNull.second);

        // two colors
        Pair pairCyanBlack = (Pair)JDOMExternUtil.readFromString(CYAN_RED_XML);
        assertEquals(Color.CYAN, pairCyanBlack.first);
        assertEquals(Color.RED, pairCyanBlack.second);

        // no colors, but null=true is not set (normally should not happen)
        Object pairWithNoColors = JDOMExternUtil.readFromString(NULL_NULL_XML);
        assertNull(pairWithNoColors);

        // no colors, null=true is set
        Object pairIsNull = JDOMExternUtil.readFromString(NULL_XML);
        assertNull(pairIsNull);
    }


    @Test
    public void testWriteColorPair() throws Exception {
        // no colors
        assertThat(
                JDOMExternUtil.writeToString("highlight", null),
                containsString(HIGHLIGHT_NULL));
        assertThat(
                JDOMExternUtil.writeToString("highlight", Pair.of(null, null)),
                containsString(HIGHLIGHT_CLASS_NULL));
        assertThat(
                JDOMExternUtil.writeToString("highlight", Pair.of(null, Color.RED)),
                containsString(HIGHLIGHT_CLASS_NULL));

        // first color only
        assertThat(
                JDOMExternUtil.writeToString("highlight", Pair.of(Color.CYAN, null)),
                containsString(HIGHLIGHT_CLASS_CYAN));

        // two colors
        assertThat(
                JDOMExternUtil.writeToString("highlight", Pair.of(Color.CYAN, Color.RED)),
                containsString(HIGHLIGHT_CLASS_CYAN_RED));
    }

    @Test
    public void testNewlines() throws Exception {
        assertEquals(Color.CYAN, JDOMExternUtil.readFromString("<root class=\"java.awt.Color\">  \n    -16711681\n  </root>"));
        assertEquals(2.0d, JDOMExternUtil.readFromString("<root class=\"java.lang.Double\">  \n    2.0\n  </root>"));
        assertEquals(2.0f, JDOMExternUtil.readFromString("<root class=\"java.lang.Float\">  \n    2.0\n  </root>"));
        assertEquals((short)2, JDOMExternUtil.readFromString("<root class=\"java.lang.Short\">  \n    2\n  </root>"));
        assertEquals(2, JDOMExternUtil.readFromString("<root class=\"java.lang.Integer\">  \n    2\n  </root>"));
        assertEquals(2l, JDOMExternUtil.readFromString("<root class=\"java.lang.Long\">  \n    2\n  </root>"));
        assertTrue((Boolean)JDOMExternUtil.readFromString("<root class=\"java.lang.Boolean\">  \n    True\n  </root>"));
        assertEquals(SampleEnum.VALUE1, JDOMExternUtil.readFromString("<root class=\"org.openclover.idea.SampleEnum\">  \n    VALUE1\n  </root>"));
    }

    @Test
    public void testRepkg() throws Exception {
        repkg.org.openclover.idea.SampleEnum rpkgval1 = repkg.org.openclover.idea.SampleEnum.VALUE1;

        String s1 = JDOMExternUtil.writeToString(rpkgval1);
        assertFalse(s1.contains("repkg."));
        Object o1 = JDOMExternUtil.readFromString(s1);
        assertSame(SampleEnum.VALUE1, o1);

        String s2 = JDOMExternUtil.writeToString(RpkgOnlyEnum.RPKG_VAL1);
        assertFalse(s2.contains("repkg."));
        Object o2 = JDOMExternUtil.readFromString(s2);
        assertSame(RpkgOnlyEnum.RPKG_VAL1, o2);
    }
}
