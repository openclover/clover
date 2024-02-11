package org.openclover.core.registry;

import org.openclover.core.api.instrumentation.ConcurrentInstrumentationException;
import org.openclover.core.instr.InstrumentationSessionImpl;

public interface InstrumentationTarget {
    RegistryUpdate applyUpdate(long expectedVersion, InstrumentationSessionImpl.Update update) throws ConcurrentInstrumentationException;
}
