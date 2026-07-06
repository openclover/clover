package org.openclover.idea;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.Topic;
import org.openclover.idea.coverage.EventListenerInstallator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

public class EventListenerInstallatorTest extends HeavyPlatformTestCase {

    // Use a custom test topic to avoid triggering IDEA 2024 internal listeners on EXECUTION_TOPIC
    // (RunToolbarPopup.MyExecutionListener etc. require RunManager which is absent in headless mode)
    private static final Topic<ExecutionListener> TEST_TOPIC =
            Topic.create("test-execution", ExecutionListener.class);

    private Project project;
    private MessageBus messageBus;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        project = getProject();
        messageBus = project.getMessageBus();
    }

    public void testSubscribeAndSyncEvent() {
        //having
        final ExecutionListener executionListener = mock(ExecutionListener.class);
        final ExecutionEnvironment executionEnvironment = mock(ExecutionEnvironment.class, withSettings().defaultAnswer(RETURNS_MOCKS));

        EventListenerInstallator.install(project, TEST_TOPIC, executionListener);
        //when
        messageBus.syncPublisher(TEST_TOPIC).processStarted("test", executionEnvironment, mock(ProcessHandler.class));

        //then
        verify(executionListener).processStarted(eq("test"), any(ExecutionEnvironment.class), any(ProcessHandler.class));
        //everything works well
    }
}
