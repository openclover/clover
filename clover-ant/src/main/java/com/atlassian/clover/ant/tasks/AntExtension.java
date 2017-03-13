package com.atlassian.clover.ant.tasks;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

import java.util.List;

public interface AntExtension {
    void resolve(Project p);
    
    List<FileSet> getFilesets();

    String getTypeName();
}
