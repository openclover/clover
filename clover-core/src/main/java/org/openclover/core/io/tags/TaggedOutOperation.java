package org.openclover.core.io.tags;

import java.io.IOException;

public interface TaggedOutOperation {
    void run(TaggedDataOutput out) throws IOException;
}
