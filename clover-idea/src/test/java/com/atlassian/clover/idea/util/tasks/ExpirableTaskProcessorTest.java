package com.atlassian.clover.idea.util.tasks;

import com.atlassian.clover.idea.util.MiscUtils;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Computable;
import com.intellij.testFramework.LightIdeaTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static clover.com.google.common.collect.Lists.newArrayList;

public class ExpirableTaskProcessorTest extends LightIdeaTestCase {

    public void testNoDuplicateTasks() {
        final ExpirableTaskProcessor etp = new ExpirableTaskProcessor();
        final AtomicBoolean firstHasRun = new AtomicBoolean();

        final ExpirableTaskDelegate slaveTaskDelegate = new AbstractTestTaskDelegate() {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                assertFalse("Inner task should be run exactly once", firstHasRun.getAndSet(true));
            }
        };

        // trick for unit tests:
        // ProgressManagerImpl works differently in normal application and in unit test mode; in unit test mode
        // we can't simply call etp.queue() twice, because ProgressManagerImpl would process them one-by-one
        // we have to add new elements to a queue while processing another task from the queue - that's why we wrap
        // slaveTaskDelegates into taskDelegate
        final ExpirableTaskDelegate taskDelegate = new AbstractTestTaskDelegate() {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                // adding new tasks to queue shall be done from dispatch thread as it would normally happen in UI
                MiscUtils.invokeAndWait(new Computable<Object>() {
                    @Override
                    public Object compute() {
                        etp.queue(slaveTaskDelegate);
                        etp.queue(slaveTaskDelegate);
                        return null;
                    }
                });
            }
        };

        etp.queue(taskDelegate);

        assertTrue("Inner task should be run exactly once", firstHasRun.get());
    }


    public void testTaskCanScheduleItselfWithOnSuccess() {
        // we want to force the same behavior for our Junit tests and production environment
        // IntelliJ in JUnit env doesn't spawn new thread and because of that our tests may blow up with
        // StackOverflowError
        final ExpirableTaskProcessor etp = new ExpirableTaskProcessor(Boolean.FALSE);
        final int N = 100000;
        final AtomicInteger globalCounter = new AtomicInteger(N);

        final ExpirableTaskDelegate taskDelegate = new AbstractTestTaskDelegate() {
            int localCounter = globalCounter.get();

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                globalCounter.decrementAndGet();
            }

            @Override
            public void onSuccess() {
                localCounter--;
                if (localCounter > 0) {
                    etp.queue(this);
                } else {
                    assertTrue("Recursive task should have ran exactly " + N + " times", globalCounter.get() == 0);
                }
            }
        };
        etp.queue(taskDelegate);
    }

    /**
     * Helper class. Just add itself to an output list.
     */
    private static class AddToListTaskDelegate extends AbstractTestTaskDelegate {
        private final List<ExpirableTaskDelegate> outputList;
        private final int id;

        public AddToListTaskDelegate(List<ExpirableTaskDelegate> outputList, int id) {
            this.outputList = outputList;
            this.id = id;
        }

        @Override
        public String toString() {
            return "#" + id;
        }

        @Override
        public void run(@NotNull ProgressIndicator progressIndicator) {
            outputList.add(this);
        }
    }

    /**
     * Helper class. Put tasks from scheduleOrder input list into to {@link ExpirableTaskProcessor#queue(ExpirableTaskDelegate)}
     */
    private static class AddTasksToQueueTaskDelegate extends AbstractTestTaskDelegate {
        private final ExpirableTaskProcessor etp;
        private final List<ExpirableTaskDelegate> scheduleOrder;

        public AddTasksToQueueTaskDelegate(ExpirableTaskProcessor etp, List<ExpirableTaskDelegate> scheduleOrder) {
            this.etp = etp;
            this.scheduleOrder = scheduleOrder;
        }

        @Override
        public void run(@NotNull ProgressIndicator progressIndicator) {
            // adding new tasks to queue shall be done from dispatch thread as it would normally happen in UI
            MiscUtils.invokeAndWait(new Computable<Object>() {
                @Override
                public Object compute() {
                    for (ExpirableTaskDelegate taskDelegate : scheduleOrder) {
                        etp.queue(taskDelegate);
                    }
                    return null;
                }
            });
        }
    }

    public void testLIFO() {
        final ExpirableTaskProcessor etp = new ExpirableTaskProcessor();

        // output list of tasks in the order of their execution
        final List<ExpirableTaskDelegate> executionOrder = newArrayList();

        // create list of tasks to be processed
        final List<ExpirableTaskDelegate> scheduleOrder = newArrayList();
        for (int i = 0; i < 5; i++) {
            scheduleOrder.add(new AddToListTaskDelegate(executionOrder, i));
        }

        // run a task, which will add another tasks to the queue

        etp.queue(new AddTasksToQueueTaskDelegate(etp, scheduleOrder));

        Collections.reverse(scheduleOrder);
        assertEquals("Execution order should be LIFO", scheduleOrder, executionOrder);
    }

    static abstract class AbstractTestTaskDelegate implements ExpirableTaskDelegate {

        @Override
        public boolean shouldProceed() {
            return true;
        }

        @Override
        public String getTitle() {
            return "Test task";
        }

        @Override
        public void onSuccess() {
        }

        @Override
        public void onCancel() {
        }
    }
}
