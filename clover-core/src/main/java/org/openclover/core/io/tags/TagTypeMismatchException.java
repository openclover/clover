package org.openclover.core.io.tags;

import java.io.IOException;

public class TagTypeMismatchException extends IOException {
    public TagTypeMismatchException(int tag, Class<? extends Class> expected, Class<? extends TaggedPersistent> found) {
        super("Unexpected class for tag " + tag + ". Expected subclass of " + expected.getName() + " but found " + found.getName());
    }
}
