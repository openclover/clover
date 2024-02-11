package org.openclover.ant.tasks;

import com.atlassian.clover.reporters.Format;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Reference;

public class CloverFormatType extends Format {
    Reference ref;

    boolean resolving = false;

    public void setRefid(Reference r) {
        this.ref = r;
    }

    public Format getActualFormat(Project p) {
        if (ref == null) {
            return this;
        }

        if (resolving) {
            throw new BuildException("Refid \"" + ref.getRefId()
                + "\" is a circular reference");
        }
        resolving = true;

        Object o = ref.getReferencedObject(p);
        if (!getClass().isAssignableFrom(o.getClass())) {
            throw new BuildException("Refid \"" + ref.getRefId()
                + "\" does not denote a format object");
        }

        CloverFormatType referencedFormat = (CloverFormatType) o;
        Format actualFormat = referencedFormat.getActualFormat(p);
        resolving = false;
        return actualFormat;
    }
}
