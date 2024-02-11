package org.openclover.core.api.optimization;

public class StringOptimizable implements Optimizable {
    private final String string;

    public StringOptimizable(String str) {
        string = str;
    }

    @Override
    public String getName() {
        return string;
    }

    @Override
    public String toString() {
        return string;
    }
}