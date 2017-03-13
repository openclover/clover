package com.atlassian.clover.io.tags;

import java.io.IOException;

public interface TaggedPersistent {
    void write(TaggedDataOutput out) throws IOException;
}
