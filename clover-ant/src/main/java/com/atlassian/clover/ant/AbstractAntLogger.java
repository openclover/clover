package com.atlassian.clover.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import com.atlassian.clover.Logger;

public abstract class AbstractAntLogger extends Logger {
    private static final int[] ANT_LOG_LEVELS = {
        Project.MSG_ERR,
        Project.MSG_WARN,
        Project.MSG_INFO,
        Project.MSG_VERBOSE,
        Project.MSG_DEBUG
    };
    protected Project proj;

    public AbstractAntLogger(Project project) {
        proj = project;
    }

    public abstract Task getTask();

    @Override
    public void log(int level, String message, Throwable exception) {
        antLog(proj, getTask(), message, exception, antLogLevel(level));
    }

    protected void antLog(Project proj, Task task, String message, Throwable exception, int antLogLevel) {
        if (task != null) {
            // no filtering here, let Ant do that
            if (exception != null) {
                proj.log(task, message, exception, antLogLevel);
            } else {
                proj.log(task, message, antLogLevel);
            }
        } else {
            // despite null task (e.g. in Gradle Clover plugin) still try to log this
            if (exception != null) {
                proj.log(message, exception, antLogLevel);
            } else {
                proj.log(message, antLogLevel);
            }
        }
    }

    protected int antLogLevel(int level) {
        return ANT_LOG_LEVELS[level];
    }
}
