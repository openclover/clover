package org.openclover.idea;

import com.intellij.ide.SelectInContext;
import com.intellij.ide.SelectInTarget;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static org.openclover.core.util.Maps.newTreeMap;

public class SelectInCloverTarget implements SelectInTarget {
    private final Map<Priority, SelectInCloverView> views = newTreeMap();

    public static SelectInCloverTarget getInstance(@NotNull Project project) {
        return (SelectInCloverTarget) project.getPicoContainer().getComponentInstanceOfType(SelectInCloverTarget.class);
    }

    public void addView(SelectInCloverView view, int priority) {
        views.put(new Priority(priority), view);
    }

    public void removeView(SelectInCloverView view) {
        views.values().remove(view);
    }

    @Override
    public boolean canSelect(SelectInContext context) {
        for (SelectInCloverView view : views.values()) {
            if (view.canSelect(context)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void selectIn(SelectInContext context, boolean requestFocus) {
        for (SelectInCloverView view : views.values()) {
            if (view.selectIn(context)) {
                return;
            }
        }
    }

    // We dont know upfront which tool window we are going to select
    @Override
    public String getToolWindowId() {
        return null;
    }

    @Override
    public String getMinorViewId() {
        return null;
    }

    @Override
    public float getWeight() {
        return Float.MAX_VALUE;
    }

    @Override
    public String toString() {
        return "Clover";
    }

    private static class Priority implements Comparable<Priority> {
        private final int priority;

        private static int tieBreakerCounter;
        @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod"})
        private final int tieBreaker = ++tieBreakerCounter;

        public Priority(int priority) {
            this.priority = priority;
        }

        @Override
        public int compareTo(Priority o) {
            return priority != o.priority ? priority - o.priority : tieBreaker - o.tieBreaker;
        }
    }
}

