package com.atlassian.clover.ant;

import clover.com.google.common.base.Function;
import clover.com.google.common.base.Joiner;
import clover.com.google.common.base.Predicate;
import clover.com.google.common.base.Predicates;
import clover.com.google.common.collect.Collections2;
import clover.org.apache.commons.lang3.ArrayUtils;
import com.atlassian.clover.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.openclover.util.Lists.newArrayList;

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


    protected static final Predicate<String> isTrimmed = new Predicate<String>() {
        @Override
        public boolean apply(@Nullable final String s) {
            return s != null && s.trim().equals(s);
        }
    };

    @NotNull
    protected static List<String> getNonTrimmedPatterns(@Nullable final String[] includePatterns,
                                                        @Nullable final String[] excludePatterns) {
        final String[] allPatterns = ArrayUtils.addAll(includePatterns, excludePatterns);
        if (allPatterns != null) {
            return newArrayList(
                    Collections2.filter(
                            newArrayList(allPatterns),
                            Predicates.not(isTrimmed)));
        } else {
            return Collections.emptyList();
        }
    }

    protected static final Function<String, String> wrapInBrackets = new Function<String, String>() {
        @Override
        public String apply(@Nullable final String s) {
            return "<" + s + ">";
        }
    };

    @NotNull
    protected static String createNonTrimmedPatternsMessage(@NotNull final Collection<String> patterns,
                                                            @NotNull final String sourceDirectory) {
        return "Attention: found inclusion/exclusion patterns for '"
                + sourceDirectory
                + "' containing leading/trailing whitespaces:\n"
                + Joiner.on("\n").join(Collections2.transform(patterns, wrapInBrackets));
    }

}
