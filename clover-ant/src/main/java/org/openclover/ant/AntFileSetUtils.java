package org.openclover.ant;

import clover.org.apache.commons.lang3.ArrayUtils;
import clover.org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openclover.runtime.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Helper class to validate Ant's FileSet and PatternSet classes
 */
public class AntFileSetUtils {

    private static Logger LOG = Logger.getInstance();

    public static void checkForNonTrimmedPatterns(@NotNull final FileSet fileSet,
                                                  @NotNull final Project project) {
        final PatternSet mergedPatterns = fileSet.mergePatterns(project);
        checkForNonTrimmedPatterns(
                mergedPatterns.getIncludePatterns(project),
                mergedPatterns.getExcludePatterns(project),
                fileSet.getDir(project).getAbsolutePath());
    }

    public static void checkForNonTrimmedPatterns(@NotNull final PatternSet patternSet,
                                                  @NotNull final Project project,
                                                  @NotNull final String sourceDirectory) {
        checkForNonTrimmedPatterns(
                patternSet.getIncludePatterns(project),
                patternSet.getExcludePatterns(project),
                sourceDirectory);
    }

    public static void checkForNonTrimmedPatterns(@Nullable final String[] includePatterns,
                                                  @Nullable final String[] excludePatterns,
                                                  @NotNull final String sourceDirectory) {
        final Collection<String> nonTrimmedPatterns = getNonTrimmedPatterns(includePatterns, excludePatterns);
        if (!nonTrimmedPatterns.isEmpty()) {
            LOG.warn(createNonTrimmedPatternsMessage(nonTrimmedPatterns, sourceDirectory));
        }
    }


    protected static final Predicate<String> isTrimmed = s -> s != null && s.trim().equals(s);

    @NotNull
    protected static List<String> getNonTrimmedPatterns(@Nullable final String[] includePatterns,
                                                        @Nullable final String[] excludePatterns) {
        final String[] allPatterns = ArrayUtils.addAll(includePatterns, excludePatterns);
        if (allPatterns != null) {
            return Arrays.asList(allPatterns).stream().filter(isTrimmed.negate()).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    protected static final Function<String, String> wrapInBrackets = s -> "<" + s + ">";

    @NotNull
    protected static String createNonTrimmedPatternsMessage(@NotNull final Collection<String> patterns,
                                                            @NotNull final String sourceDirectory) {
        return "Attention: found inclusion/exclusion patterns for '"
                + sourceDirectory
                + "' containing leading/trailing whitespaces:\n"
                + StringUtils.join(patterns.stream().map(wrapInBrackets), "\n");
    }

}
