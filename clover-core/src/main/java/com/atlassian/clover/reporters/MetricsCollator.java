package com.atlassian.clover.reporters;

import com.atlassian.clover.api.registry.ClassInfo;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.registry.metrics.HasMetricsSupport;
import com.atlassian.clover.registry.util.EntityVisitorUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.openclover.util.Lists.newArrayList;
import static org.openclover.util.Maps.newTreeMap;

/**
 * A class indepedent of any specific formatters, used simply to collate
 * and organise data to be presented to the user.
 */
public class MetricsCollator {

    final HasMetricsFilter methodFilter = hasMetrics ->
            hasMetrics.getMetrics().getPcCoveredElements() != 1 &&
                    hasMetrics.getMetrics().getNumElements() > 0;

    final EntityVisitorUtils entityUtils = new EntityVisitorUtils();

    /**
     * Given a list of {@link com.atlassian.clover.api.registry.ClassInfo} objects,
     * return a list of {@link com.atlassian.clover.api.registry.MethodInfo} s, ranked
     * by PC COvered (asc), Num Elements Uncovered and Complexity.
     *
     * @return List&lt;MethodInfo&gt;
     */
    public List<MethodInfo> getLeastTestedMethods(final List<? extends ClassInfo> classes,
                                                  final boolean showLambdaFunctions,
                                                  final boolean showInnerFunctions) {
        // get the 10 most untested yet most complex methods
        // get all methods with no coverage
        final List<MethodInfo> methodsLeastTested = newArrayList();
        int numMethods = 20;

        // can use classes - filtered on test classes and coverage == 100%
        for (ClassInfo classInfo : classes) {
            for (MethodInfo methodInfo : classInfo.getAllMethods()) {
                if (methodFilter.accept(methodInfo)
                        && (showLambdaFunctions || !methodInfo.isLambda())
                        && (showInnerFunctions || !entityUtils.isInnerMethod(methodInfo))
                ) {
                    methodsLeastTested.add(methodInfo);
                }
            }
        }

        final List<HasMetricsSupport.HasMetricsComparator> comparators = newArrayList();
        comparators.add(HasMetricsSupport.CMP_PC_ASC);
        comparators.add(HasMetricsSupport.CMP_ELEMENTS_UNCOVERED_DESC);
        comparators.add(HasMetricsSupport.CMP_COMPLEXITY_DESC);
        final HasMetricsSupport.HasMetricsComparator cmp = new HasMetricsSupport.HasMetricsComparatorChain(comparators);
        Collections.sort(methodsLeastTested, cmp);
        final int toIndex = methodsLeastTested.size() < numMethods ? methodsLeastTested.size() : numMethods;
        return methodsLeastTested.subList(0, toIndex);
    }


    public List getTopRisks(Map<Integer, List<ClassInfo>> classMap, int risksCount) {
        int count = 0;
        final List topRisks = new ArrayList(risksCount);
        for (Integer key : classMap.keySet()) {
            List list = classMap.get(key);

            final int size = list.size();
            if ((count + size) < risksCount) {
                topRisks.addAll(list);
            } else {
                int diff = risksCount - (count);
                topRisks.addAll(list.subList(0, diff));
                break;
            }
            count += size;
        }
        return topRisks;
    }

    /**
     * Sums the index of the class when ordered by %covered elements and that when ordered
     * by average method complexity. This gives a single 'risk value' that is then the key
     * in the returned map.
     *
     * @param pceOrder a list of ClassInfos ordered by percentage of elements covered
     * @param amcOrder a list of ClassInfos ordered by average method complexity
     * @return an ordered map keyed on Integer (risk value), value List of ClassInfo.
     */
    public Map<Integer, List<ClassInfo>> rankProjectRisks(List<? extends ClassInfo> pceOrder, List amcOrder) {
        final Map<Integer, List<ClassInfo>> classMap = newTreeMap();
        int i = 0;
        for (ClassInfo info : pceOrder) {
            final int rank = amcOrder.indexOf(info) + (++i);
            final Integer key = rank;
            final List<ClassInfo> value;
            if (classMap.containsKey(key)) {
                value = classMap.get(key);
            } else {
                value = newArrayList();
                classMap.put(key, value);
            }
            value.add(info);
        }
        return classMap;
    }

    public List getTopOfList(List infos, int topNCount, HasMetricsSupport.HasMetricsComparator cmp) {
        Collections.sort(infos, cmp);
        Collections.reverse(infos);
        int last = infos.size() >= topNCount ? topNCount : infos.size();
        return infos.subList(0, last);
    }


}
