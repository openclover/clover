package org.openclover.idea.config.regexp;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * RegexpValidator Tester.
 */
public class RegexpValidatorTest extends TestCase {

    private static final String[] INVALID_NAMES = { null, "", " ", "static", "private", "with space", "with,comma",
                                                    " x", "x ", "  ", "tab\t" };
    private static final String[] INVALID_PATTERNS = { null, "", "\\", "*" };

    public void testValidate() throws Exception {
        RegexpValidator validator = new RegexpValidator();

        Regexp regexp = new Regexp();
        regexp.setName("validName");
        regexp.setRegex(".*");

        validator.validate(regexp);
        assertTrue("Initial regexp should be valid", regexp.isValid());

        for (String name : INVALID_NAMES) {
            regexp.setName(name);
            validator.validate(regexp);
            assertFalse("Regexp with invalid name", regexp.isValid());
        }

        regexp.setName("validName");
        regexp.setRegex(".*");

        validator.validate(regexp);
        assertTrue("Initial regexp should be valid", regexp.isValid());
        
        for (String pattern : INVALID_PATTERNS) {
            regexp.setRegex(pattern);
            validator.validate(regexp);
            assertFalse("Regexp with invalid pattern", regexp.isValid());
        }


    }

    public static Test suite() {
        return new TestSuite(RegexpValidatorTest.class);
    }
}
