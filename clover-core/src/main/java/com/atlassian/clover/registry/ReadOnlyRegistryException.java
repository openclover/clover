package com.atlassian.clover.registry;

import com.atlassian.clover.api.registry.CloverRegistryException;

public class ReadOnlyRegistryException extends CloverRegistryException {
    public ReadOnlyRegistryException() {
        super(
            "This database can only be used for reporting because it is the result of a merge."
            + " It does not support further coverage gathering.");
    }
}
