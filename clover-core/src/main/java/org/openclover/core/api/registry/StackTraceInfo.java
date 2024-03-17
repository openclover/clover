package org.openclover.core.api.registry;

import java.util.List;

public interface StackTraceInfo {

    List<StackTraceEntry> getEntries();

    TestCaseInfo getOriginatingTest();

    void setOriginatingTest(TestCaseInfo originatingTest);

    void resolve(ProjectInfo proj);

}
