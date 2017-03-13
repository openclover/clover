package com.atlassian.clover.io.tags;

import java.io.IOException;

public interface TaggedOutOperation {
    public void run(TaggedDataOutput out) throws IOException;
}
