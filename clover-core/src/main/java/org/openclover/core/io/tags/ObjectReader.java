package org.openclover.core.io.tags;

import java.io.IOException;

public interface ObjectReader<T extends TaggedPersistent> {
    T read(TaggedDataInput in) throws IOException;
}
