package org.openclover.core.io.tags;

import java.io.IOException;

public class WrongTagException extends IOException {
    public WrongTagException(int expectedTag, int actualTag) {
        super("Expected tag: " + Integer.toHexString(expectedTag) + " Actual Tag: " + Integer.toHexString(actualTag));
    }
}
