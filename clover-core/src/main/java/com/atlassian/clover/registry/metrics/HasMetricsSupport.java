package com.atlassian.clover.registry.metrics;

import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.api.registry.BlockMetrics;
import com.atlassian.clover.util.NamedComparator;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import static clover.com.google.common.collect.Lists.newArrayList;
import static clover.com.google.common.collect.Maps.newHashMap;

public class HasMetricsSupport {
    /** Lexical HasMetrics Comparator  **/
    public static final HasMetricsComparator CMP_LEX = new HasMetricsComparator() {

        @Override
        public int compare(HasMetrics obj1, HasMetrics obj2) {
            return obj1.getName().compareTo(obj2.getName());
        }

        @Override
        public String getName() {
            return "ALPHA";
        }

        @Override
        public String getDescription() {
            return "Alphabetical";
        }

    };


    public static final HasMetricsComparator CMP_COMPLEXITY = new HasMetricsComparator() {
        @Override
        public int compare(HasMetrics obj1, HasMetrics obj2) {
            Integer t1 = obj1.getMetrics().getComplexity();
            Integer t2 = obj2.getMetrics().getComplexity();
            return t1.compareTo(t2);
        }

        @Override
        public String getName() {
            return "COMPLEXITY";
        }

        @Override
        public String getDescription() {
            return "cyclomatic complexity, ascending";
        }
    };

    public static final HasMetricsComparator CMP_COMPLEXITY_LEX = new HasMetricsComparatorChain(
            new HasMetricsComparator[]{CMP_COMPLEXITY, CMP_LEX});

    public static final HasMetricsComparator CMP_COMPLEXITY_DESC = new HasMetricsComparator() {

        @Override
        public String getName() {
            return "COMPLEXITYDESC";
        }

        @Override
        public String getDescription() {
            return "cyclomatic complexity, descending";
        }

        @Override
        public int compare(HasMetrics obj1, HasMetrics obj2) {
            return -1 * CMP_COMPLEXITY.compare(obj1, obj2);
        }
    };

    public static final HasMetricsComparator CMP_ELEMENTS_COVERED_ASC = new HasMetricsComparator() {
        @Override
        public int compare(HasMetrics obj1, HasMetrics obj2) {
            Integer t1 = obj1.getMetrics().getNumCoveredElements();
            Integer t2 = obj2.getMetrics().getNumCoveredElements();
            return t1.compareTo(t2);
        }

        @Override
        public String getName() {
            return "ELEMENTSCOVEREDASC";
        }

        @Override
        public String getDescription() {
            return "elements covered, ascending";
        }

    };

    public static HasMetricsComparator CMP_ELEMENTS_COVERED_LEX_ASC =
            new HasMetricsComparatorChain(new HasMetricsComparator[]{CMP_ELEMENTS_COVERED_ASC, CMP_LEX});

    public static final HasMetricsComparator CMP_ELEMENTS_COVERED_DESC
            = new ReverseComparator(CMP_ELEMENTS_COVERED_ASC, "ELEMENTSCOVEREDDESC", "elements covered, descending");

    public static final HasMetricsComparator CMP_ELEMENTS_UNCOVERED_ASC = new HasMetricsComparator() {
        @Override
        public int compare(HasMetrics obj1, HasMetrics obj2) {
            Integer t1 = obj1.getMetrics().getNumUncoveredElements();
            Integer t2 = obj2.getMetrics().getNumUncoveredElements();
            return t1.compareTo(t2);
        }

        @Override
        public String getName() {
            return "ELEMENTSUNCOVEREDASC";
        }

        @Override
        public String getDescription() {
            return "elements uncovered, ascending";
        }
    };

    public static final HasMetricsComparator CMP_ELEMENTS_UNCOVERED_LEX_ASC =
            new HasMetricsComparatorChain(new HasMetricsComparator[] {CMP_ELEMENTS_UNCOVERED_ASC, CMP_LEX});

    public static final HasMetricsComparator CMP_ELEMENTS_UNCOVERED_DESC =
                        new HasMetricsComparator() {
        @Override
        public String getName() {
            return "ELEMENTSUNCOVEREDDESC";
        }

        @Override
        public String getDescription() {
            return "elements uncovered, descending";
        }

        @Override
        public int compare(HasMetrics obj1, HasMetrics obj2) {
            return -1 * CMP_ELEMENTS_UNCOVERED_ASC.compare(obj1, obj2);
        }
    };

    public static final HasMetricsComparator CMP_PC_ASC = new HasMetricsComparator() {
        @Override
        public int compare(HasMetrics obj1, HasMetrics obj2) {
           Float t1 = new Float(obj1.getMetrics().getPcCoveredElements());
           Float t2 = new Float(obj2.getMetrics().getPcCoveredElements());
           return t1.compareTo(t2);
        }
        @Override
        public String getName() {
            return "PCASC";
        }
        @Override
        public String getDescription() {
            return "Percentag total Coverage, ascending";
        }
    };

    /** HasMetrics Percentage Ascending Comparator **/
    public static final HasMetricsComparator CMP_PC_LEX_ASC =
            new HasMetricsComparatorChain(
                    new HasMetricsComparator[] {CMP_PC_ASC, CMP_LEX});

    private static final Map<String, HasMetricsComparator> COMPARATORS = newHashMap();
    private static final HasMetricsComparator CMP_DEFAULT = CMP_PC_LEX_ASC;

    public static final HasMetricsComparator CMP_PC_LEX_DESC
            = new ReverseComparator(CMP_PC_LEX_ASC, "PCCOVEREDDESC", "Percent total coverage, descending");

    public static final String CMP_DEFAULT_NAME = CMP_DEFAULT.getName().toUpperCase(Locale.ENGLISH);

    static {
        // names should always be uppercase
        COMPARATORS.put(
                HasMetricsSupport.CMP_LEX.getName().toUpperCase(Locale.ENGLISH),
                HasMetricsSupport.CMP_LEX);
        COMPARATORS.put(
                HasMetricsSupport.CMP_PC_LEX_ASC.getName().toUpperCase(Locale.ENGLISH),
                HasMetricsSupport.CMP_PC_LEX_ASC);
        COMPARATORS.put(
                HasMetricsSupport.CMP_PC_LEX_DESC.getName().toUpperCase(Locale.ENGLISH),
                HasMetricsSupport.CMP_PC_LEX_DESC);
        COMPARATORS.put(
                HasMetricsSupport.CMP_ELEMENTS_COVERED_ASC.getName().toUpperCase(Locale.ENGLISH),
                HasMetricsSupport.CMP_ELEMENTS_COVERED_ASC);
        COMPARATORS.put(
                HasMetricsSupport.CMP_ELEMENTS_COVERED_DESC.getName().toUpperCase(Locale.ENGLISH),
                HasMetricsSupport.CMP_ELEMENTS_COVERED_DESC);
        COMPARATORS.put(
                HasMetricsSupport.CMP_ELEMENTS_UNCOVERED_ASC.getName().toUpperCase(Locale.ENGLISH),
                HasMetricsSupport.CMP_ELEMENTS_UNCOVERED_ASC);
        COMPARATORS.put(
                HasMetricsSupport.CMP_ELEMENTS_UNCOVERED_DESC.getName().toUpperCase(Locale.ENGLISH),
                HasMetricsSupport.CMP_ELEMENTS_UNCOVERED_DESC);
    }

    public static final HasMetricsComparator LEX_COMP =
        new HasMetricsComparator() {
            @Override
            public int compare(HasMetrics obj1, HasMetrics obj2) {
                if (obj1 == null && obj2 == null) {
                    return 0;
                } else if (obj1 == null) {
                    return -1;
                } else if (obj2 == null) {
                    return 1;
                } else {
                    String n1 = obj1.getName();
                    String n2 = obj2.getName();
                    return n1.compareTo(n2);
                }
            }

            @Override
            public String getName() {
                return "alpha";
            }

            @Override
            public String getDescription() {
                return "alphabetical";
            }
        };

    /** Percentage Ascending Comparator for <code>CoverageUnits</code>* */
    public static final HasMetricsComparator PC_ASCENDING_COMP
         = new CoverageComparator(1, "pcasc", "Percent total coverage, ascending");

    /** Percentage Descending Comparator for <code>CoverageUnits</code>* */
    public static final HasMetricsComparator PC_DESCENDING_COMP
         = new CoverageComparator(-1, "pcdesc", "Percent total coverage, descending");

    public static HasMetricsComparator getHasMetricsComparator(String name) {
        HasMetricsComparator cmp = CMP_DEFAULT;

        if (name != null && name.length() > 0) {
            if (name.contains(" ") || name.contains(",")) {
                return buildComparatorChain(name);
            }
            cmp = COMPARATORS.get(name.toUpperCase(Locale.ENGLISH));
            if (cmp == null) {
                cmp = CMP_DEFAULT;
            }
        }
        return cmp;
    }

    private static HasMetricsComparatorChain buildComparatorChain(String list) {
        List<HasMetricsComparator> cmps = newArrayList();
        for (StringTokenizer toks = new StringTokenizer(list, " ,"); toks.hasMoreTokens();) {
            String tok = toks.nextToken();
            cmps.add(getHasMetricsComparator(tok));
        }

        return new HasMetricsComparatorChain(cmps);
    }

    /**
     * Helper class for comparing <code>CoverageUnits</code>
     */
    static class CoverageComparator extends HasMetricsComparator {
        /** the direction of ordering * */
        private int direction;
        private String name;
        private String desc;

        /**
         * build an instance for the given direction
         */
        public CoverageComparator(int direction, String name, String desc) {
            this.direction = direction;
            this.name = name;
            this.desc = desc;
        }

        @Override
        public int compare(HasMetrics obj1, HasMetrics obj2) {
            if (obj1 == null && obj2 == null) {
                return 0;
            } else if (obj1 == null) {
                return -1;
            } else if (obj2 == null) {
                return 1;
            } else {
                float s1 = obj1.getMetrics().getPcCoveredElements();
                float s2 = obj2.getMetrics().getPcCoveredElements();

                if (s1 == s2) {
                    // secondary lexigraphical sort
                    return LEX_COMP.compare(obj1, obj2);
                } else if (s1 == -1) {
                    return -direction;
                } else if (s2 == -1) {
                    return direction;
                } else if (s1 > s2) {
                    return direction;
                } else {
                    return -direction;
                }
            }
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return desc;
        }
    }

    static class ReverseComparator extends HasMetricsComparator {

        private final HasMetricsComparator cmp;
        private final String name;
        private final String desc;

        public ReverseComparator(HasMetricsComparator cmp, String name, String desc) {
            this.cmp = cmp;
            this.name = name;
            this.desc = desc;
        }

        @Override
        public int compare(HasMetrics obj1, HasMetrics obj2) {
            return -1 * cmp.compare(obj1, obj2);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return desc;
        }
    }

    public static abstract class HasMetricsComparator implements NamedComparator<HasMetrics> {

        @Override
        public abstract int compare(HasMetrics obj1, HasMetrics obj2);

        @Override
        public abstract String getName();

        @Override
        public abstract String getDescription();
    }

    public static class HasMetricsComparatorChain extends HasMetricsComparator {
        private HasMetricsComparator[] chain;
        private String name;
        private String description;

        public HasMetricsComparatorChain(List chain) {
            this((HasMetricsComparator[]) chain.toArray(new HasMetricsComparator[0]), null, null);
        }

        public HasMetricsComparatorChain(HasMetricsComparator[] chain, String name, String desc) {
            this.chain = chain;
            this.name = name;
            this.description = desc;
        }

        /**
         * Constructor that sets name and description to that of the first
         * comparator in the array.
         * @param chain the comparators to chain
         */
        public HasMetricsComparatorChain(HasMetricsComparator[] chain) {
            this.chain = chain;
            if (chain.length > 0) {
                this.name = chain[0].getName();
                this.description = chain[0].getDescription();
            }
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public int compare(HasMetrics obj1, HasMetrics obj2) {
            for (HasMetricsComparator comparator : chain) {
                int result = comparator.compare(obj1, obj2);
                if (result != 0) {
                    return result;
                }
            }

            return 0;
        }
    }



    static abstract class MetricsComparator extends HasMetricsComparator {
        public abstract int compare(BlockMetrics m, BlockMetrics m1);

        @Override
        public int compare(HasMetrics obj1, HasMetrics obj2) {
            return compare(obj1.getMetrics(), obj2.getMetrics());
        }
    }

    public static final Comparator CMP_PC_TESTS_PASS = new MetricsComparator() {
        @Override
        public int compare(BlockMetrics m, BlockMetrics m1) {
            return (int) (1000 * (m.getPcTestPasses() - m1.getPcTestPasses()));
        }
        @Override
        public String getName() { return "PCTESTSPASS"; }
        @Override
        public String getDescription() { return "% of tests that passed";}
    };

    public static final Comparator CMP_TESTS_FAIL = new MetricsComparator() {
        @Override
        public int compare(BlockMetrics m, BlockMetrics m1) {
            return m.getNumTestFailures() - m1.getNumTestFailures();
        }
        @Override
        public String getName() { return "NUMTESTSFAIL"; }
        @Override
        public String getDescription() { return "Number of tests that failed";}

    };

    public static final Comparator CMP_TESTS_ERROR = new MetricsComparator() {
        @Override
        public int compare(BlockMetrics m, BlockMetrics m1) {
            return m.getNumTestErrors() - m1.getNumTestErrors();
        }
        @Override
        public String getName() { return "NUMTESTSERROR"; }
        @Override
        public String getDescription() { return "Number of tests that had errors";}
    };

    static final Comparator[] TEST_COMPARATORS = new Comparator[] {CMP_PC_TESTS_PASS, CMP_TESTS_FAIL, CMP_TESTS_ERROR};

    public static Comparator newTestListComparator() {
        return new HasMetricsComparatorChain(Arrays.asList(TEST_COMPARATORS));
    }

}
