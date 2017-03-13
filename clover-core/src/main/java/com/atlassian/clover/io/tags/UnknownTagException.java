package com.atlassian.clover.io.tags;

import java.io.IOException;

public class UnknownTagException extends IOException {
    public UnknownTagException(String className) {
        super("No tag registered for " + className);
    }

    public UnknownTagException(int tag) {
        super("No builder registered for tag " + tag);
    }
}
