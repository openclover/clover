package com.atlassian.clover.idea;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;

public class ApplicationTestHelper {
    public interface Action {
        void run() throws Exception;
    }

    public static void runWriteAction(final Action task) throws Exception {
        final Exception e = ApplicationManager.getApplication().runWriteAction(new Computable<Exception>() {
            @Override
            public Exception compute() {
                try {
                    task.run();
                } catch (Exception ex) {
                    return ex;
                }
                return null;
            }
        });
        if (e != null) {
            throw e;
        }
    }
}
