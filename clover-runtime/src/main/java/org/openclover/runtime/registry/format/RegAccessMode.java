package org.openclover.runtime.registry.format;

public enum RegAccessMode {
    READWRITE(0),
    READONLY(1);

    private final int value;

    RegAccessMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static RegAccessMode getFor(int value) {
        if (value == 1) {
            return READONLY;
        } else {
            return READWRITE;
        }
    }
}
