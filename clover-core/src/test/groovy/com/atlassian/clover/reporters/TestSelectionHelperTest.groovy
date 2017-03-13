package com.atlassian.clover.reporters

import com.atlassian.clover.reporters.filters.AntPatternTestFilter
import org.hamcrest.Description
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatcher
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

import static org.mockito.Mockito.verifyZeroInteractions

class TestSelectionHelperTest {
    private static final String[][][] TESTS = [
            [
                    [ "-one", "param", "--testsIncludePattern", "pattern" ],
                    [ "AntPatternTestFilter:[null][pattern][]"]
            ],
            [
                    [ "-one", "param", "--testsIncludePattern", "incPat", "--testsExcludePattern", "excPat", "--sourceRoot", "${File.separator}srcRoot" ],
                    [ "AntPatternTestFilter:[${File.separator}srcRoot${File.separator}][incPat][excPat]" ]
            ],
            [
                    [ "-one", "param", "--testsIncludePattern", "incPat,incPat2", "--testsExcludePattern", "excPat, excPat2", "--sourceRoot", "${File.separator}srcRoot" ],
                    [ "AntPatternTestFilter:[${File.separator}srcRoot${File.separator}][incPat, incPat2][excPat, excPat2]" ]
            ],
            [
                    [ "--sourceRoot", File.separator + "srcRoot" + File.separator, "--testsIncludePattern", "incPat" ],
                    [ "AntPatternTestFilter:[${File.separator}srcRoot${File.separator}][incPat][]" ]
            ],
    ]

    @Mock
    Current cfg

    @Before
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    void testConfigureWithoutParams() {
        final String[] p1 = [ "-one", "--two", "param" ]
        TestSelectionHelper.configureTestSelectionFilter(cfg, p1)
        verifyZeroInteractions(cfg)
    }

    @Test
    void testConfigureTestSelectionFilter() {
        for (String[][] test : TESTS) {
            final String[] args = test[0]
            final String pattern = test[1][0]

            TestSelectionHelper.configureTestSelectionFilter(cfg, args)
            Mockito.verify(cfg).setTestFilter(Mockito.argThat(new Matches(pattern)))
        }
    }

    static class Matches extends ArgumentMatcher<AntPatternTestFilter> {
        private final String string
        Matches(String s) {
            string = s
        }

        boolean matches(Object argument) {
            return string.equals(argument.toString())
        }

        @Override
        void describeTo(Description description) {
            description.appendText(string)
        }
    }
}
