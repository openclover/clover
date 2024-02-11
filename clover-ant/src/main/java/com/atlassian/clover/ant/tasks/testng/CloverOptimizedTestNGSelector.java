package com.atlassian.clover.ant.tasks.testng;

import org.openclover.runtime.CloverNames;
import com.atlassian.clover.api.optimization.Optimizable;
import com.atlassian.clover.api.optimization.OptimizationOptions;
import com.atlassian.clover.optimization.Optimizer;
import com.atlassian.clover.optimization.OptimizationSession;
import com.atlassian.clover.optimization.LocalSnapshotOptimizer;
import com.atlassian.clover.optimization.Snapshot;
import org.testng.IAnnotationTransformer2;
import org.testng.annotations.IConfigurationAnnotation;
import org.testng.annotations.IDataProviderAnnotation;
import org.testng.annotations.IFactoryAnnotation;
import org.testng.annotations.ITestAnnotation;
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestOrConfiguration;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.io.File;

import static org.openclover.util.Lists.newArrayList;

public class CloverOptimizedTestNGSelector implements IAnnotationTransformer, IAnnotationTransformer2 {
    private Optimizer optimizer;
    private OptimizationSession session;

    // IAnnotationTransformer

    @Override // signature in TestNG 5.9+
    public void transform(ITestAnnotation annotation, Class clazz, Constructor constructor, Method method) {
        maybeInitializeOptimizer();
        if (optimizer.canOptimize()) {
            Optimizable testable = testableFor(clazz, constructor, method);
            if (testable == null || !optimizer.include(testable, session)) {
                return;
            }
        }
        addCloverOptimizedToGroup(annotation);
    }

    // IAnnotationTransformer2

    /**
     * Add "clover-optimized" group to any method annotated with any of "@BeforeTest / @BeforeSuite / @AfterTest
     * / @AfterSuite" etc.
     */
    @Override
    public void transform(IConfigurationAnnotation annotation, Class aClass, Constructor constructor, Method method) {
        addCloverOptimizedToGroup(annotation);
    }

    @Override
    public void transform(IDataProviderAnnotation annotation, Method method) {

    }

    @Override
    public void transform(IFactoryAnnotation annotation, Method method) {

    }

    // internal

    private void addCloverOptimizedToGroup(ITestOrConfiguration annotation) {
        String[] groups = annotation.getGroups();
        if (groups == null || groups.length == 0) {
            groups = new String[] {"clover-optimized"};
        } else {
            List<String> groupsAsList = newArrayList(groups);
            groupsAsList.add("clover-optimized");
            groups = groupsAsList.toArray(new String[0]);
        }
        annotation.setGroups(groups);
    }

    private Optimizable testableFor(Class clazz, Constructor constructor, Method method) {
        Class daRealClazz =
            clazz == null
                ? method == null
                    ? constructor == null
                        ? null
                        : constructor.getDeclaringClass()
                    : method.getDeclaringClass()
                : clazz;

        if (daRealClazz == null) {
            return null;
        } else {
            final StringBuilder name = new StringBuilder(daRealClazz.getName());
            if (name.indexOf("$") > -1) {
                name.delete(name.indexOf("$"), name.length());
            }
            return name::toString;
        }
    }

    public void maybeInitializeOptimizer() {
        if (optimizer == null) {
            String snapshotPath = System.getProperty(CloverNames.PROP_TEST_SNAPSHOT);
            String initString = System.getProperty(CloverNames.PROP_INITSTRING);
            final OptimizationOptions options =
                new OptimizationOptions.Builder()
                    .optimizableName("class")
                    .snapshot(
                        snapshotPath == null
                            ? initString == null
                                ? null
                                : Snapshot.fileForInitString(initString)
                            : new File(snapshotPath))
                    .initString(initString).build();
            optimizer = new LocalSnapshotOptimizer(options);
            session = new OptimizationSession(options, false);
        }
    }
}
