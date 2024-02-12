package org.openclover.core.util

import org.openclover.runtime.Logger
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

import static org.openclover.core.util.Lists.newArrayList

/**
 * A Logger that will record what got logged.
 * This is to be only used for unit testing, since if left set will eventually cause OOME.
 */
class RecordingLogger extends Logger {
    static final class LogMessage {
        String msg
        int level
        Throwable t

        LogMessage(int level, String msg,  Throwable t) {
            this.msg = msg
            this.level = level
            this.t = t
        }

        @Override
        String toString() {
            return "LogMessage{" +
                "level=" + level +
                ", msg='" + msg + '\'' +
                ", t=" + t +
                '}'
        }
    }

    static class MessageContainsMatcher extends TypeSafeMatcher<LogMessage> {
        private String expectedMessageFragment

        public MessageContainsMatcher(String expectedMessageFragment) {
            this.expectedMessageFragment = expectedMessageFragment
        }

        @Override
        protected boolean matchesSafely(RecordingLogger.LogMessage logMessage) {
            return logMessage.msg.contains(expectedMessageFragment)
        }

        @Override
        void describeTo(Description description) {
            description.appendText("does not contain message fragment ").appendValue(expectedMessageFragment)
        }
    }

    static class MessageEqualsMatcher extends TypeSafeMatcher<LogMessage> {
        private String expectedMessage

        public MessageEqualsMatcher(String expectedMessage) {
            this.expectedMessage = expectedMessage
        }

        @Override
        protected boolean matchesSafely(RecordingLogger.LogMessage logMessage) {
            return logMessage.msg.equals(expectedMessage)
        }

        @Override
        void describeTo(Description description) {
            description.appendText("does not have message ").appendValue(expectedMessage)
        }
    }

    static class ThrowableEqualsMatcher extends TypeSafeMatcher<LogMessage> {
        private Throwable expectedThrowable

        public ThrowableEqualsMatcher(Throwable expectedThrowable) {
            this.expectedThrowable = expectedThrowable
        }

        @Override
        protected boolean matchesSafely(RecordingLogger.LogMessage logMessage) {
            return logMessage.t.equals(expectedThrowable)
        }

        @Override
        void describeTo(Description description) {
            description.appendText("does not have throwable ").appendValue(expectedThrowable)
        }
    }


    private final List<LogMessage> buffer = Collections.synchronizedList(newArrayList())

    void log(int level, String msg, Throwable t) {
        buffer.add(new LogMessage(level, msg, t))
    }

    void reset() {
        buffer.removeAll(buffer)
    }

    boolean find(TypeSafeMatcher<LogMessage> m) {
        synchronized (buffer) {
            for (LogMessage logMessage : buffer) {
                if (m.matches(logMessage)) {
                    return true
                }
            }
        }
        return false
    }

    boolean contains(final String message) {
        return find(new MessageEqualsMatcher(message))
    }

    boolean contains(final Throwable t) {
        return find(new ThrowableEqualsMatcher(t))
    }

    boolean containsFragment(final String fragment) {
        return find(new MessageContainsMatcher(fragment))
    }

    List<? extends Object> getBuffer() {
        return buffer
    }

}
