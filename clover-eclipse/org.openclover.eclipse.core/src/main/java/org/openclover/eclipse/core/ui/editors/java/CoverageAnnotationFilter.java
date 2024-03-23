package org.openclover.eclipse.core.ui.editors.java;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.openclover.core.CloverDatabase;
import org.openclover.core.CoverageData;
import org.openclover.core.api.registry.BranchInfo;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.ContextSet;
import org.openclover.core.api.registry.ElementInfo;
import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.api.registry.CoverageDataRange;
import org.openclover.core.api.registry.TestCaseInfo;
import org.openclover.core.registry.entities.FullElementInfo;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.eclipse.core.CloverPlugin;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.openclover.core.util.Sets.newHashSet;
import static org.openclover.eclipse.core.CloverPlugin.logError;

public class CoverageAnnotationFilter {
    public static final QualifiedName EXCLUDED_TEST_NAMES = new QualifiedName(CloverPlugin.ID, "CoverageAnnotationExcludedTestNames");

    public static CoverageAnnotationFilter NULL = new CoverageAnnotationFilter() {
        @Override
        public boolean includes(SourceInfo info) { return true; }
    };

    public static CoverageAnnotationFilter loadFor(
            CloverDatabase database, FullFileInfo fileInfo,
            Map<TestCaseInfo, BitSet> tcisAndHitsForFile, IResource editedResource,
            boolean hideCovered, ContextSet blockFilter) {

        return new CoverageAnnotationFilter(
            database,
            fileInfo,
            tcisAndHitsForFile,
            TestFilter.loadFor(editedResource, EXCLUDED_TEST_NAMES),
            hideCovered,
            blockFilter);
    }

    private CloverDatabase database;
    private Set<TestCaseInfo> includedTests;
    private TestFilter excludedTestNames;
    private boolean filterOutFullyCovered;
    private ContextSet blockFilter;
    private Map<TestCaseInfo, BitSet> tcisAndCoverageForFile;

    private CoverageAnnotationFilter() {}

    private CoverageAnnotationFilter(
        CloverDatabase database,
        FullFileInfo fileInfo,
        Map<TestCaseInfo, BitSet> tcisAndCoverageForFile,
        TestFilter testNames,
        boolean filterOutFullyCovered,
        ContextSet blockFilter) {
        
        this.database = database;
        this.excludedTestNames = testNames;
        this.blockFilter = blockFilter;
        this.filterOutFullyCovered = filterOutFullyCovered;
        this.tcisAndCoverageForFile = tcisAndCoverageForFile;

        Set<TestCaseInfo> includedTests = newHashSet(tcisAndCoverageForFile.keySet());
        for (Iterator<TestCaseInfo> iterator = includedTests.iterator(); iterator.hasNext();) {
            TestCaseInfo testCase = iterator.next();
            if (testCase != null) {
                if (testCase.isResolved() || testCase.resolve(fileInfo.getContainingPackage().getContainingProject())) {
                    if (testNames.matches(testCase)) {
                        iterator.remove();
                    }
                }
            }
        }
        this.includedTests = includedTests;
    }

    public boolean includes(SourceInfo info) {
        included: {
            if (info instanceof BranchInfo) {
                BranchInfo branchInfo = (BranchInfo)info;

                if (filterOutFullyCovered && (branchInfo.getTrueHitCount() > 0) && (branchInfo.getFalseHitCount() > 0)) {
                    break included;
                }
            } else if (info instanceof ElementInfo) {
                ElementInfo elementInfo = (ElementInfo)info;

                if (filterOutFullyCovered && (elementInfo.getHitCount() > 0)) {
                    break included;
                }
            }

            //Partially covered branches should always be shown regardless of filtering
            if (info instanceof CoverageDataRange
                && !isPartiallyCoveredBranch(info)) {

                Set<TestCaseInfo> hittingTcis = CoverageData.tcisInHitRange(tcisAndCoverageForFile, (CoverageDataRange)info);
                Set<TestCaseInfo> filteredHits = newHashSet(hittingTcis);
                filteredHits.retainAll(includedTests);

                //After filtering out hits were there any left?
                //(filteredHits.size() == 0)
                //Was this just because there were no
                //hits to start with (hits.size() > 0)
                if (filteredHits.size() == 0 && hittingTcis.size() > 0) {
                    break included;
                }
            }

            //We passed the inclusion tests, time to indicate this
            return true;
        }

        //We failed one or more inclusion tests
        return false;
    }

    public boolean isHidden(SourceInfo info) {
        return
            (info instanceof FullElementInfo
            && (((FullElementInfo)info).isFiltered(blockFilter)));
    }

    private boolean isPartiallyCoveredBranch(SourceInfo info) {
        return
            (info instanceof BranchInfo
             && (((BranchInfo)info).getTrueHitCount() == 0
                 || ((BranchInfo)info).getFalseHitCount() == 0));
    }

    public static class TestFilter {
        private Set<Integer> ids;
        private Set<String> testClassNames;

        public TestFilter() {
            this(new HashSet<>(), new HashSet<>());
        }

        public TestFilter(Set<Integer> ids, Set<String> testClassNames) {
            this.ids = ids;
            this.testClassNames = testClassNames;
        }

        public void addTestCase(TestCaseInfo test) {
            ids.add(test.getId());
        }

        public void removeTestCase(TestCaseInfo test) {
            ids.remove(test.getId());
        }

        public void addTestClass(ClassInfo clazz) {
            testClassNames.add(clazz.getQualifiedName());
        }

        public void removeTestClass(ClassInfo clazz) {
            testClassNames.remove(clazz.getQualifiedName());
        }

        public static TestFilter loadFor(IResource editedResource, QualifiedName propertyName) {
            Set<Integer> testIds = newHashSet();
            Set<String> testClasses = newHashSet();
            if (editedResource != null) {
                try {
                    String testIdsAsString = (String)editedResource.getSessionProperty(propertyName);
                    if (testIdsAsString != null) {
                        String[] testIdStrings = testIdsAsString.split(",");
                        for (String testIdString : testIdStrings) {
                            try {
                                testIds.add(Integer.valueOf(testIdString));
                            } catch (NumberFormatException e) {
                                testClasses.add(testIdString);
                            }
                        }
                    }
                } catch (CoreException e) {
                    logError("Unable to query filtered out test ids/classes for " + editedResource, e);
                }
            }
            return new TestFilter(testIds, testClasses);
        }

        public void saveFor(IResource editedResource, QualifiedName propertyName) {
            try {
                StringBuilder value = new StringBuilder();
                for (Iterator<Integer> iterator = ids.iterator(); iterator.hasNext();) {
                    value.append(iterator.next());
                    if (iterator.hasNext() || testClassNames.size() > 0) {
                        value.append(',');
                    }
                }
                for (Iterator<String> iterator = testClassNames.iterator(); iterator.hasNext();) {
                    value.append(iterator.next());
                    if (iterator.hasNext()) {
                        value.append(',');
                    }
                }

                editedResource.setSessionProperty(propertyName, value.toString());
            } catch (CoreException e) {
                logError("Unable to save test ids/classes for " + editedResource, e);
            }
        }

        public static void removeFor(IResource resource, QualifiedName name) {
            try{
                resource.setSessionProperty(name, null);
            } catch (CoreException e) {
                logError("Unable to remove test names for " + resource, e);
            }
        }

        public boolean matches(TestCaseInfo testCaseInfo) {
            return
                (testCaseInfo.getRuntimeType() == null
                    ? false
                    : testClassNames.contains(testCaseInfo.getRuntimeType().getQualifiedName()))
                || ids.contains(testCaseInfo.getId());
        }

        public boolean isEmpty() {
            return
                testClassNames.isEmpty()
                && ids.isEmpty();
        }
    }
}
