package com.atlassian.clover.idea.coverage;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

public class EventListenerInstallator {

    public static <T> void install(final @NotNull Project project, final @NotNull Topic<T> topic, @NotNull final T listener) throws IllegalStateException {
        final MessageBus messageBus = project.getMessageBus();
        final MessageBusConnection connect = messageBus.connect(project);
        connect.subscribe(topic, listener);

        // we don't need to hold reference to connet object in order to disconnect when connection/listeners
        // will not be used anymore because of the MessageBusConnection#connect(Disposable) docs saying it's going to
        // be attached to the disposable and disposed when parent will be disposed, in that particular case, when
        // project will be closed
    }

}
