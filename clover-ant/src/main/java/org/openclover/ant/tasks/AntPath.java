package org.openclover.ant.tasks;

import org.openclover.core.util.Path;


public class AntPath extends Path
{
    public AntPath(org.apache.tools.ant.types.Path antPath) {
        super(antPath.list());
    }
}
