package com.atlassian.clover.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.openclover.runtime.Logger;


public class AntLogger extends AbstractAntLogger {
    private Task task;

    public AntLogger(Project aProj, Task aTask) {
        super(aProj);
        task = aTask;
    }

    @Override
    public Task getTask() {
        return task;
    }

    public static class Factory implements Logger.Factory {

        private Project project;
        private Task task;

        public Factory(Project project, Task task) {
            this.project = project;
            this.task = task;
        }

        @Override
        public Logger getLoggerInstance(String category) {
            return new AntLogger(project, task);
        }
    }

}


