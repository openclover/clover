package com.atlassian.clover.idea.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.util.Computable;

import java.util.concurrent.atomic.AtomicReference;

public class MiscUtils {
    private MiscUtils() {
    }

    public static <T> T invokeAndWait(final Computable<T> computable) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            return computable.compute();
        }

        final AtomicReference<T> holder = new AtomicReference<T>();
        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            @Override
            public void run() {
                holder.set(computable.compute());
            }
        }, ModalityState.defaultModalityState());
        return holder.get();
    }

    public static void invokeLater(Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable);
    }

    /**
     * Invoke code in a UI thread, locking it for an write action and wait until terminates.
     * @param runnable code to run
     */
    public static void invokeWriteActionAndWait(final Runnable runnable) {
        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(runnable);
            }
        }, ModalityState.defaultModalityState());
    }
}
