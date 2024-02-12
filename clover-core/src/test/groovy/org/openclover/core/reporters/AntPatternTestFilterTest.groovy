package org.openclover.core.reporters

import org.junit.Test
import org.openclover.core.instr.java.InstrumentationState
import org.openclover.core.instr.java.JavaTypeContext
import org.openclover.core.instr.tests.AntPatternTestDetectorFilter
import org.openclover.core.registry.entities.FullClassInfo
import org.openclover.core.registry.entities.FullFileInfo
import org.openclover.core.reporters.filters.AntPatternTestFilter
import org.openclover.core.util.FilterUtils

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class AntPatternTestFilterTest {
    static String PROJECT_ROOT = toOSSensitivePath("/home/user/project")
    
    static String[][] PATTERNS = [
            // include pattern, exclude pattern, matching classes, not matching classes
            [ "", "", "", "UtilityClass.java, Test.java SomeTest.java" ],
            [ "**/*Test.java", "", "Test.java SomeTest.java", "UtilityClass.java" ],
            [ "**/*Test.java, **/*Utility*.java", "", "Test.java SomeTest.java UtilityClass.java", "Other.java" ],
            [ "**/*Test.java", "", toOSSensitivePath("/home/user/project/src/pkg/SomeTest.java"), toOSSensitivePath("/home/user/project/src/pkg/SomeClass.java") ]
    ]

    /**
     * Converts UNIX style path to OS sensitive path - paths used for testing must match the filesystem path naming
     * or else the tests will fail because we use File.getPath() in the filter.
     */
    private static String toOSSensitivePath(String path) {
        return path.replace("/", File.separator)
    }

    @Test
    void testAcceptFilter() {
        for (String[] pattern : PATTERNS) {
            final String[] include = FilterUtils.tokenizePattern(pattern[0])
            final String[] exclude = FilterUtils.tokenizePattern(pattern[1])
            final String[] matching = pattern[2].split(" ")
            final String[] nonMatching = pattern[3].split(" ")

            AntPatternTestFilter filter = new AntPatternTestFilter(PROJECT_ROOT, include, exclude)
            for (String file : matching) {
                if (file.length() == 0) continue
                assertTrue("Expecting match: " + file, filter.accept(makeClassInfo(file)))
            }

            for (String file : nonMatching) {
                if (file.length() == 0) continue
                assertFalse("Expecting not-match: " + file, filter.accept(makeClassInfo(file)))
            }
        }
    }

    @Test
    void testAcceptTestDetector() {
        for (String[] pattern : PATTERNS) {
            final String[] include = FilterUtils.tokenizePattern(pattern[0])
            final String[] exclude = FilterUtils.tokenizePattern(pattern[1])
            final String[] matching = pattern[2].split(" ")
            final String[] nonMatching = pattern[3].split(" ")

            AntPatternTestDetectorFilter filter = new AntPatternTestDetectorFilter(new File(PROJECT_ROOT), include, exclude)
            for (String file : matching) {
                if (file.length() == 0) continue
                assertTrue("Expecting match: " + file, filter.isTypeMatch(makeStateInfo(file), new JavaTypeContext(null, null, null, null, null)))
            }

            for (String file : nonMatching) {
                if (file.length() == 0) continue
                assertFalse("Expecting not-match: " + file, filter.isTypeMatch(makeStateInfo(file), new JavaTypeContext(null, null, null, null, null)))
            }
        }
    }

    private static FullClassInfo makeClassInfo(String file) {
        FullFileInfo fi = mock(FullFileInfo.class)
        when(fi.getPhysicalFile()).thenReturn(new File(file))

        FullClassInfo ci = mock(FullClassInfo.class)
        when(ci.getContainingFile()).thenReturn(fi)
        return ci
    }

    private static InstrumentationState makeStateInfo(String file) {
        InstrumentationState state = mock(InstrumentationState.class)
        // absolute or relative path
        when(state.getSourceFile()).thenReturn(file.startsWith(File.separator) ? new File(file) : new File(PROJECT_ROOT, file))
        return state
    }
}
