package org.openclover.core.optimization;

import org.openclover.core.api.optimization.Optimizable;

/**
 * An optimizable which is backed by a class.
 * The name of the optimizable is the name of the class.
 */
public class ClassOptimizable implements Optimizable {

    private final Class<?> mClass;

    public ClassOptimizable(Class<?> clazz) {
        mClass = clazz;
    }

    @Override
    public String getName() {
        return mClass.getName();
    }

    public Class<?> getMyClass() {
        return mClass;
    }
}