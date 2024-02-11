package com.atlassian.clover.instr.java

import org.openclover.runtime.recorder.pertest.SnifferType
import org.openclover.runtime.remote.DistributedConfig
import com_atlassian_clover.CloverProfile
import org.junit.Test

import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.text.MatchesPattern.matchesPattern
import static org.junit.Assert.assertEquals
import static org.hamcrest.MatcherAssert.assertThat
import static org.openclover.util.Lists.newArrayList

/**
 * Test for {@link RecorderInstrEmitter}
 */
class RecorderInstrEmitterTest {

    /** "default" written in unicode (without quotes) */
    String defaultAsUnicode = "\\u0064\\u0065\\u0066\\u0061\\u0075\\u006c\\u0074"

    /** "shared" written in unicode (without quotes) */
    String sharedAsUnicode = "\\u0073\\u0068\\u0061\\u0072\\u0065\\u0064"

    /** Sample configuration string */
    String distributedConfigStr = "name=my-config;host=myhost;port=1111;timeout=555;numClients=10;retryPeriod=111"
    String distributedConfigStrSorted = "host=myhost;name=my-config;numClients=10;port=1111;retryPeriod=111;timeout=555"

    /**
     * Test for {@link RecorderInstrEmitter#asUnicodeString(String)}
     * @throws Exception
     */
    @Test
    void testAsUnicodeString() throws Exception {
        // simple ascii chars
        assertEquals("\"\\u0061\\u0062\\u0063\"", RecorderInstrEmitter.asUnicodeString("abc"))
        // ascii with backslash
        assertEquals("\"\\u005c\\u005c\\u0074\\u0065\\u0061\"", RecorderInstrEmitter.asUnicodeString("\\tea"))
        // ascii with escape character
        assertEquals("\"\\u0009\\u0065\\u0061\"", RecorderInstrEmitter.asUnicodeString("\tea"))
        // ascii with forward slash
        assertEquals("\"\\u002f\\u0072\\u006f\\u006f\\u0074\"", RecorderInstrEmitter.asUnicodeString("/root"))
        // empty string - we shall have "" in output
        assertEquals("\"\"", RecorderInstrEmitter.asUnicodeString(""))
        // null value - we shall have [null] in output
        assertEquals("null", RecorderInstrEmitter.asUnicodeString(null))
    }

    /**
     * Test for {@link RecorderInstrEmitter#generateCloverProfilesField(java.util.List)}
     * - empty profiles array
     */
    @Test
    void testGenerateCloverProfilesFieldEmptyProfiles() {
        String expected = "public static " + CloverProfile.class.getName() + "[] profiles = { };"
        String actual = RecorderInstrEmitter.generateCloverProfilesField(newArrayList())
        assertEquals(expected, actual)
    }

    /**
     * Test for {@link RecorderInstrEmitter#generateCloverProfilesField(java.util.List)}
     * - empty profiles array
     */
    @Test
    void testGenerateCloverProfilesFieldNullProfiles() {
        String expected = "public static " + CloverProfile.class.getName() + "[] profiles = { };"
        assertEquals(expected, RecorderInstrEmitter.generateCloverProfilesField(null))
    }

    /**
     * Test for {@link RecorderInstrEmitter#generateCloverProfilesField(java.util.List)}
     * - null as third param
     */
    @Test
    void testGenerateCloverProfilesFieldOneProfileWithNullDistributed() {

        String expected = "public static " + CloverProfile.class.getName() + "[] profiles = { " +
                "new " + CloverProfile.class.getName() + "(\"" + defaultAsUnicode + "\", \"FIXED\", null)" +
                "};"
        List<CloverProfile> profiles = newArrayList()
        profiles.add(new CloverProfile("default", CloverProfile.CoverageRecorderType.FIXED, null))

        assertEquals(expected, RecorderInstrEmitter.generateCloverProfilesField(profiles))
    }

    /**
     * Test for {@link RecorderInstrEmitter#generateCloverProfilesField(java.util.List)}
     * - distributed config string as 3rd param
     */
    @Test
    void testGenerateCloverProfilesFieldOneProfileWithDistributed() {
        String expected = "public static " + CloverProfile.class.getName() + "[] profiles = { " +
                "new " + CloverProfile.class.getName() + "(\"" + defaultAsUnicode + "\", \"FIXED\", " +
                RecorderInstrEmitter.asUnicodeString(distributedConfigStrSorted) + ")" +
                "};"

        List<CloverProfile> profiles = newArrayList()
        profiles.add(new CloverProfile("default", CloverProfile.CoverageRecorderType.FIXED,
                new DistributedConfig(distributedConfigStr)))

        assertEquals(expected, RecorderInstrEmitter.generateCloverProfilesField(profiles))
    }

    /**
     * Test for {@link RecorderInstrEmitter#generateCloverProfilesField(java.util.List)}
     * - multiple profiles separated by comma
     */
    @Test
    void testGenerateCloverProfilesFieldManyProfiles() {
        String expected = "public static " + CloverProfile.class.getName() + "[] profiles = { " +
                "new " + CloverProfile.class.getName() + "(\"" + defaultAsUnicode + "\", \"FIXED\", " +
                RecorderInstrEmitter.asUnicodeString(distributedConfigStrSorted) + ")," /*with comma*/ +
                "new " + CloverProfile.class.getName() + "(\"" + sharedAsUnicode + "\", \"SHARED\", null)" +
                "};"

        List<CloverProfile> profiles = newArrayList()
        profiles.add(new CloverProfile("default", CloverProfile.CoverageRecorderType.FIXED,
                new DistributedConfig(distributedConfigStr)))
        profiles.add(new CloverProfile("shared", CloverProfile.CoverageRecorderType.SHARED, null))

        assertEquals(expected, RecorderInstrEmitter.generateCloverProfilesField(profiles))
    }

    @Test
    void testGenerateCloverProfilesInlineManyProfiles() {
        String expected = "new " + CloverProfile.class.getName() + "[] {" +
                "new " + CloverProfile.class.getName() + "(\"" + defaultAsUnicode + "\", \"FIXED\", " +
                RecorderInstrEmitter.asUnicodeString(distributedConfigStrSorted) + ")," /*with comma*/ +
                "new " + CloverProfile.class.getName() + "(\"" + sharedAsUnicode + "\", \"SHARED\", null)" +
                "}"

        List<CloverProfile> profiles = newArrayList()
        profiles.add(new CloverProfile("default", CloverProfile.CoverageRecorderType.FIXED,
                new DistributedConfig(distributedConfigStr)))
        profiles.add(new CloverProfile("shared", CloverProfile.CoverageRecorderType.SHARED, null))

        assertEquals(expected, RecorderInstrEmitter.generateCloverProfilesInline(profiles))
    }

    /**
     * Test whether a TestNameSniffer field is added and initialized to
     * {@link com_atlassian_clover.TestNameSniffer#NULL_INSTANCE} or
     * {@link com_atlassian_clover.TestNameSniffer.Simple} or
     */
    @Test
    void testGenerateTestSnifferField() {
        String expectedNull = '^public static final .*TestNameSniffer __CLR.*TEST_NAME_SNIFFER=.*TestNameSniffer\\.NULL_INSTANCE;$'
        String actualNull = RecorderInstrEmitter.generateTestSnifferField(SnifferType.NULL)
        assertThat(actualNull, matchesPattern(expectedNull))

        String expectedJUnit = '^public static final .*TestNameSniffer __CLR.*TEST_NAME_SNIFFER=new .*TestNameSniffer\\.Simple\\(\\);$'
        String actualJUnit = RecorderInstrEmitter.generateTestSnifferField(SnifferType.JUNIT)
        assertThat(actualJUnit, matchesPattern(expectedJUnit))
    }

    @Test
    void testGenerateTestSnifferField_Bool_Bool() {
        String actualNull = RecorderInstrEmitter.generateTestSnifferField(false, false, false)
        assertThat(actualNull, containsString("NULL_INSTANCE"))

        String actualJUnit = RecorderInstrEmitter.generateTestSnifferField(false, true, false)
        assertThat(actualJUnit, containsString("TestNameSniffer.Simple"))

        String actualSpock = RecorderInstrEmitter.generateTestSnifferField(true, false, false)
        assertThat(actualSpock, containsString("TestNameSniffer.Simple"))
    }

}