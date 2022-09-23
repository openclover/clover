package com.atlassian.clover.io.tags;

import java.io.IOException;

public interface TaggedOutOperation {
    void run(TaggedDataOutput out) throws IOException;
}
