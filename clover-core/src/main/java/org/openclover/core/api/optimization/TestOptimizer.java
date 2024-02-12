package org.openclover.core.api.optimization;

import org.openclover.core.optimization.ClassOptimizable;
import org.openclover.core.optimization.LocalSnapshotOptimizer;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A TestOptimizer allows you to optimize a Collection of classes (that are Tests) programatically.
 * <p>
 * Typically, this is to be done if your tests in are defined in a junit.framework.TestSuite.
 * </p>
 * <ul>
 * <li>The {@link #optimize} method will take a list of Optimizables and return an optimized list of Optimizables.</li>
 * <li>The {@link #optimizeTestClasses} method is a convenience method for optimizing a list of Test Classes.</li>
 * </ul>
 */
public class TestOptimizer {

    private final OptimizationOptions options;

    public TestOptimizer(final OptimizationOptions options) {
        this.options = options;
    }

    public TestOptimizer(final String initString, final File snapshotFile) {
        this.options = new OptimizationOptions.Builder().debug(true).initString(initString).snapshot(snapshotFile).build();
    }

    /**
     * Optimize a collection of Test classes.
     *
     * @param testClasses classes to be optimized
     * @return an optimized list of Classes
     */
    public Collection<Class<?>> optimizeTestClasses(final Collection<Class<?>> testClasses) {

        final List<Optimizable> optimizableClasses= convertClassesToOptimizables(testClasses);
        final List<? extends Optimizable> optimizedClasses = optimize(optimizableClasses);
        return convertClassList(optimizedClasses);
    }


    /**
     * Optimize a collection of any Optimizable.
     * 
     * @param optimizables a list of objects, denoting tests, to optimize
     * @param <T> Optimizable
     * @return an optimized list of optimizable objects
     */
    public <T extends Optimizable> List<T> optimize(final List<T> optimizables) {
        try {
            // actually optimize
            return LocalSnapshotOptimizer.optimize(optimizables, options);
            
        } catch (CloverException e) {
            Logger.getInstance().error("Optimization failed. Running all specified tests.");
            Logger.getInstance().debug(e.getMessage(), e);

        }
        return optimizables;
    }

    /**
     * <p>Optimizes a collection of unknown objects. It tries to call 'getName()' method on each element from
     * <code>objectList</code> using reflections. It returns a List where each element is an Optimizable
     * which getName() method returns a value of corresponding <code>objectList.get(x).getName()</code>.</p>
     *
     * <p>This is a workaround for Clover-for-Grails plugin, where FileOptimizable class cannot
     * implement Optimizable interface due to problems with dependency resolution in Grails
     * via 'install-plugin' command. We're using Object and reflections to pass the necessary information.</p>
     *
     * @param objectList list of objects having the 'String getName()' method returning name of optimizable resource
     * @return List&lt;Optimizable&gt; optimized resource set
     * @throws java.lang.NoSuchMethodException if getName() method does not exist
     * @throws java.lang.reflect.InvocationTargetException if getName() call fails
     * @throws java.lang.IllegalAccessException if getName() call fails
     */
    public List<Optimizable> optimizeObjects(final List<?> objectList) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final List<Optimizable> list = new ArrayList<>(objectList.size());
        for (final Object obj : objectList) {
            // name = obj.getName();
            final String name = (String)obj.getClass().getMethod("getName").invoke(obj);
            list.add(new StringOptimizable(name));
        }

        // perform optimization
        return optimize(list);
    }


    private Collection<Class<?>> convertClassList(final List<? extends Optimizable> optimizables) {
        // create the list of culled and re-ordered classes.
        final Collection<Class<?>> optimizedClasses = new ArrayList<>(optimizables.size());
        for (final Optimizable optimizable : optimizables) {
            // lookup the class for each optimizable
            final ClassOptimizable optClass = (ClassOptimizable) optimizable;
            optimizedClasses.add(optClass.getMyClass());
        }
        return optimizedClasses;
    }

    /**
     * Converts a collection of classes to a Map of Optimizables, Class.
     * 
     * @param testClasses the list of classes to convert
     * @return a map of classes, keyed on Optimizable.
     */
    private List<Optimizable> convertClassesToOptimizables(final Collection<Class<?>> testClasses) {

        final List<Optimizable> optimizables = new ArrayList<>(testClasses.size());
        for (final Class<?> aClass : testClasses) {
            optimizables.add(new ClassOptimizable(aClass));
        }
        return optimizables;
    }


}
