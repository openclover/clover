package com.atlassian.clover.idea;

import com.atlassian.clover.idea.coverage.EventListenerInstallator;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.IdeaTestCase;
import com.intellij.util.messages.MessageBus;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

public class EventListenerInstallatorTest extends IdeaTestCase {

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

        EventListenerInstallator.install(project, ExecutionManager.EXECUTION_TOPIC, executionListener);
        //when
        messageBus.syncPublisher(ExecutionManager.EXECUTION_TOPIC).processStarted("test", executionEnvironment, mock(ProcessHandler.class));

        //then
        verify(executionListener).processStarted(eq("test"), any(ExecutionEnvironment.class), any(ProcessHandler.class));
        //everything works well
    }
}
