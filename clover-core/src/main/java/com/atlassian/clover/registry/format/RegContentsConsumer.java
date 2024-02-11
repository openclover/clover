package com.atlassian.clover.registry.format;

import org.openclover.runtime.api.registry.CloverRegistryException;

import java.io.IOException;

public interface RegContentsConsumer {
    void consume(RegContents contents) throws IOException, CloverRegistryException;
}
