package com.atlassian.clover.ant.tasks;

import com.atlassian.clover.util.Path;


public class AntPath extends Path
{
    public AntPath(org.apache.tools.ant.types.Path antPath) {
        super(antPath.list());
    }
}
