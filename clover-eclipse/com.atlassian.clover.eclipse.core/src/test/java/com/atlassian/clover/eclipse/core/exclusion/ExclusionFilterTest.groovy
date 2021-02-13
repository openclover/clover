package com.atlassian.clover.eclipse.core.exclusion

import com.atlassian.clover.eclipse.core.projects.settings.source.SourceFolderPattern
import org.junit.Test

import static org.junit.Assert.assertEquals

class ExclusionFilterTest {
    String[][] testSet1 = [
            [ "src", "**/*.include", "**/*.exclude", "+" ],
            [ "src1", "invalid", "invalid", "-" ]
    ]

    String[] includeSet1 = [
            "src/**/*.include"
    ]

    String[] excludeSet1 = [
            "src/**/*.exclude"
    ]

    private static List<SourceFolderPattern> translate(String[][] testData) {
        List<SourceFolderPattern> list = new ArrayList<SourceFolderPattern>(testData.length)
        for (String[] testRow : testData) {
            list.add(new SourceFolderPattern(
                    testRow[0], testRow[1], testRow[2], "+".equals(testRow[3])))
        }
        return list
    }

    @Test
    void testCalculateIncludeFilter() {
        String[] patterns = ExclusionFilter.calculateIncludeFilter(translate(testSet1))
        assertEquals(Arrays.asList(includeSet1), Arrays.asList(patterns))
    }

    @Test
    void testCalculateExcludeFilter() {
        String[] patterns = ExclusionFilter.calculateExcludeFilter(translate(testSet1))
        assertEquals(Arrays.asList(excludeSet1), Arrays.asList(patterns))

    }

    String[][][] unrollData = [
            [
                    [ "src", "simple" ], [ "src/simple" ]
            ],
            [
                    [ "src/sub", "simple" ], [ "src/sub/simple" ]
            ],
            [
                    [ "src", "simple,another,next" ], [ "src/simple", "src/another", "src/next" ]
            ],
            [
                    [ "src", "/slashed" ], [ "src/slashed" ]
            ],
            [
                    [ "src", "unslashed,/slashed" ], [ "src/unslashed", "src/slashed" ]
            ],
            [
                    [ "src", "" ], [] /* empty pattern means match nothing */
            ],
            [
                    [ "src", ",," ], []
            ],
            [
                    [ "src", " trim, excess ,  spaces" ], [ "src/trim", "src/excess", "src/spaces" ]
            ],
            [
                    [ "src", "/ but, not /*.all " ], [ "src/ but", "src/not /*.all" ]
            ]
    ]

    @Test
    void testUnroll() {
        for (String[][] testCase : unrollData) {
            String folder = testCase[0][0]
            String pattern = testCase[0][1]
            String[] expectedResult = testCase[1]

            List<String> result = ExclusionFilter.unroll(folder, pattern)

            assertEquals("[$folder][$pattern]", Arrays.asList(expectedResult), result)
        }

    }
}
