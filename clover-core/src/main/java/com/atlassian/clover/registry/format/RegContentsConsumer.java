package com.atlassian.clover.registry.format;

import com.atlassian.clover.api.registry.CloverRegistryException;

import java.io.IOException;

public interface RegContentsConsumer {
    public void consume(RegContents contents) throws IOException, CloverRegistryException;
}
