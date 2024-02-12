package org.openclover.ant.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Reference;
import org.openclover.core.reporters.Columns;

/**
 * A helper class to resolve refids in Ant build files.
 */
public class ColumnsEx extends Columns {

    Reference ref;

    boolean resolving = false;

    public void setRefid(Reference r) {
        this.ref = r;
    }

    public ColumnsEx resolveColumnsRef() {
        if (ref == null) {
            return this;
        }

        if (resolving) {
            throw new BuildException("Refid \"" + ref.getRefId()
                + "\" is a circular reference");
        }
        resolving = true;

        Object o = ref.getReferencedObject();
        if (!getClass().isAssignableFrom(o.getClass())) {
            throw new BuildException("Refid \"" + ref.getRefId()
                + "\" does not denote a columns element.");
        }

        ColumnsEx referencedCols = (ColumnsEx) o;
        ColumnsEx actualCols = referencedCols.resolveColumnsRef();
        resolving = false;
        return actualCols;
    }
}
