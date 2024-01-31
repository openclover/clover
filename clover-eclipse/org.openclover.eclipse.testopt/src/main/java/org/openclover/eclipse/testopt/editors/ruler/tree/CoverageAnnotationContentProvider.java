package org.openclover.eclipse.testopt.editors.ruler.tree;

import java.util.Collection;
import java.util.Map;

import org.openclover.eclipse.testopt.editors.ruler.CoverageAnnotationRulerHover;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.atlassian.clover.registry.entities.TestCaseInfo;

import static org.openclover.util.Maps.newHashMap;

public class CoverageAnnotationContentProvider implements ITreeContentProvider {

    private final Map<String, Map<String, TestCaseInfo>> data = newHashMap();
    
    @Override
    public Object[] getChildren(Object parentElement) {
        final Map<String, TestCaseInfo> map = data.get(parentElement);
        return map == null ? null : map.values().toArray();
    }

    @Override
    public Object getParent(Object element) {
        if (element instanceof TestCaseInfo) {
            return ((TestCaseInfo)element).getRuntimeTypeName();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasChildren(Object element) {
        return data.containsKey(element);
    }

    @Override
    public Object[] getElements(Object inputElement) {
        return data.keySet().toArray();
    }

    @Override
    public void dispose() {}

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object inputElement) {
        data.clear();
        if (inputElement instanceof CoverageAnnotationRulerHover.CoverageAnnotationInput) {
            final Collection<TestCaseInfo> testCases = ((CoverageAnnotationRulerHover.CoverageAnnotationInput)inputElement).testCases;
            
            for (TestCaseInfo tci: testCases) {
                final String testClass = tci.getRuntimeTypeName();
                Map<String, TestCaseInfo> tests = data.computeIfAbsent(testClass, k -> newHashMap());
                final String testName = tci.getSourceMethodName();
                final TestCaseInfo prev = tests.get(testName);
                if (prev == null || prev.getEndTime() < tci.getEndTime()) {
                    tests.put(testName, tci);
                }
            }
        }
    }

}
