package org.openclover.core.reporters;

import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.api.registry.HasMetricsFilter;
import org.openclover.core.registry.metrics.HasMetricsSupport;
import org.openclover.core.registry.util.EntityVisitorUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.openclover.core.util.Lists.newArrayList;
import static org.openclover.core.util.Maps.newTreeMap;

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
     * Given a list of {@link ClassInfo} objects,
     * return a list of {@link MethodInfo} s, ranked
     * by PC COvered (asc), Num Elements Uncovered and Complexity.
     *
     * @return List&lt;MethodInfo&gt;
     */
    public List<MethodInfo> getLeastTestedMethods(final List<ClassInfo> classes,
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
        methodsLeastTested.sort(cmp);
        final int toIndex = methodsLeastTested.size() < numMethods ? methodsLeastTested.size() : numMethods;
        return methodsLeastTested.subList(0, toIndex);
    }


    public <T extends ClassInfo> List<T> getTopRisks(Map<Integer, List<T>> classMap, int risksCount) {
        int count = 0;
        final List<T> topRisks = new ArrayList<>(risksCount);
        for (Integer key : classMap.keySet()) {
            List<T> list = classMap.get(key);

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
    public Map<Integer, List<ClassInfo>> rankProjectRisks(List<ClassInfo> pceOrder,
                                                          List<ClassInfo> amcOrder) {
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

    public <T extends HasMetrics> List<T> getTopOfList(List<T> infos, int topNCount, HasMetricsSupport.HasMetricsComparator cmp) {
        infos.sort(cmp);
        Collections.reverse(infos);
        int last = infos.size() >= topNCount ? topNCount : infos.size();
        return infos.subList(0, last);
    }


}
