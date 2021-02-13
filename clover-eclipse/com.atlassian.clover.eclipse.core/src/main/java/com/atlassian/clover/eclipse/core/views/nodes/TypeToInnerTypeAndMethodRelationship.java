package com.atlassian.clover.eclipse.core.views.nodes;

import com.atlassian.clover.eclipse.core.CloverPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static clover.com.google.common.collect.Lists.newArrayList;

public class TypeToInnerTypeAndMethodRelationship extends NodeRelationship {
    @Override
    public Object[] getChildren(Object object, NodeRelationshipFilter filter) {
        try {
            //Ignore all but inner-type definitions and methods (e.g. no imports, package statements)
            List<IType> types = newArrayList(((IType) object).getTypes()); // copy
            for(Iterator<IType> iter = types.iterator(); iter.hasNext();) {
                IType type = iter.next();
                if (type.isAnonymous()) {
                    iter.remove();
                }
            }
            IMethod[] methods = ((IType) object).getMethods();
            Collection children = new ArrayList(types.size() + methods.length);
            children.addAll(types);
            children.addAll(Arrays.asList(methods));
            return filter.perform(children);
        } catch (CoreException e) {
            CloverPlugin.logError("Unable to collect inner-types in type " + object, e);
            return new Object[]{};
        }
    }

    @Override
    public boolean includes(Object object) {
        return object instanceof IType;
    }
}
