package org.openclover.core.io.tags;

import java.io.IOException;

public interface TaggedPersistent {
    void write(TaggedDataOutput out) throws IOException;
}
