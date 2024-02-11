package org.openclover.runtime.recorder;

import java.io.IOException;

/**
 * A per-test coverage recording from the currently running application
 */
public interface LivePerTestRecording extends PerTestRecording {
    /**
     * Transcribe the coverage in some way, returning a memento of the transcription
     */
    String transcribe() throws IOException;

    LivePerTestRecording NULL = new LivePerTestRecording() {
        ///CLOVER:OFF
        @Override
        public String getTestTypeName() {
            return null;
        }

        @Override
        public String getTestMethodName() {
            return null;
        }

        @Override
        public String getRuntimeTestName() {
            return null;
        }

        @Override
        public int getExitStatus() {
            return PerTestRecorder.NO_EXIT_RESULT;
        }

        @Override
        public long getStart() {
            return 0;
        }

        @Override
        public long getEnd() {
            return 0;
        }

        @Override
        public double getDuration() {
            return 0;
        }

        @Override
        public boolean hasResult() {
            return false;
        }

        @Override
        public boolean isResultPassed() {
            return false;
        }

        @Override
        public String getStackTrace() {
            return null;
        }

        @Override
        public String getExitMessage() {
            return null;
        }

        @Override
        public String transcribe() {
            return null;
        }

        @Override
        public long getDbVersion() {
            return 0;
        }

        @Override
        public int getFormat() {
            return PerTestRecording.FORMAT;
        }
        ///CLOVER:ON
    };

}
