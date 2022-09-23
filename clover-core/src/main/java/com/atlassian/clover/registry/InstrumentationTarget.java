package com.atlassian.clover.registry;

import com.atlassian.clover.registry.RegistryUpdate;
import com.atlassian.clover.api.instrumentation.ConcurrentInstrumentationException;
import com.atlassian.clover.instr.InstrumentationSessionImpl;

public interface InstrumentationTarget {
    RegistryUpdate applyUpdate(long expectedVersion, InstrumentationSessionImpl.Update update) throws ConcurrentInstrumentationException;
}
