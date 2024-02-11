package org.openclover.core.registry;

import org.openclover.runtime.api.registry.CloverRegistryException;

public class ReadOnlyRegistryException extends CloverRegistryException {
    public ReadOnlyRegistryException() {
        super(
            "This database can only be used for reporting because it is the result of a merge."
            + " It does not support further coverage gathering.");
    }
}
