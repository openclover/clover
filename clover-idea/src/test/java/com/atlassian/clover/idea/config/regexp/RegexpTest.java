package org.openclover.idea.config.regexp;

import org.openclover.idea.config.ContextFilterRegexpType;
import com.atlassian.clover.context.MethodRegexpContext;
import com.atlassian.clover.context.StatementRegexpContext;
import junit.framework.TestCase;

import java.util.regex.Pattern;

/**
 * Regexp Tester.
 */
public class RegexpTest extends TestCase {
    public void testIsDifferent() {

        MethodRegexpContext m = new MethodRegexpContext("name", Pattern.compile(".*"));
        StatementRegexpContext s = new StatementRegexpContext("name", Pattern.compile(".*"));

        Regexp r = getRegexp("name", ".*", ContextFilterRegexpType.Method.ordinal()); // something stinks here...

        assertTrue(r.isDifferent(null));

        assertFalse(r.isDifferent(m));
        assertTrue(getRegexp("name", ".*", ContextFilterRegexpType.Statement.ordinal()).isDifferent(m));
        assertTrue(getRegexp("name", ".*.", ContextFilterRegexpType.Method.ordinal()).isDifferent(m));
        assertTrue(getRegexp("name1", ".*", ContextFilterRegexpType.Method.ordinal()).isDifferent(m));

        assertFalse(getRegexp("name", ".*", ContextFilterRegexpType.Statement.ordinal()).isDifferent(s));
        assertTrue(getRegexp("name", ".*", ContextFilterRegexpType.Method.ordinal()).isDifferent(s));
        assertTrue(getRegexp("name", ".*.", ContextFilterRegexpType.Statement.ordinal()).isDifferent(s));
        assertTrue(getRegexp("name1", ".*", ContextFilterRegexpType.Statement.ordinal()).isDifferent(s));

        // CIJ-174
        Regexp unfinished = new Regexp();
        unfinished.setName("name");
        assertTrue(unfinished.isDifferent(m));

        MethodRegexpContext m1 = new MethodRegexpContext("name", null); // just in case
        assertFalse(unfinished.isDifferent(m1));

    }

    private static Regexp getRegexp(String name, String regexp, int type) {
        final Regexp r = new Regexp();
        r.setName(name);
        r.setRegex(regexp);
        r.setType(type);
        return r;
    }
}
