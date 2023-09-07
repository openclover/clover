package com.atlassian.clover.idea.build;

import com.intellij.openapi.compiler.ValidityState;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class CloverValidityState implements ValidityState {
    private final boolean included;

    public CloverValidityState(boolean included) {
        this.included = included;
    }

    @Override
    public boolean equalsTo(ValidityState otherState) {
        if (otherState instanceof CloverValidityState) {
            final CloverValidityState other = (CloverValidityState) otherState;
            return other.included == included;
        } else {
            return false;
        }
    }

    /**
     * A workaround for http://youtrack.jetbrains.com/issue/IDEA-122924 (for IDEA 13.1.x)
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof ValidityState ? equalsTo((ValidityState)other) : super.equals(other);
    }

    public static CloverValidityState read(DataInput in) throws IOException {
        return new CloverValidityState(in.readBoolean());
    }

    @Override
    public void save(DataOutput out) throws IOException {
        out.writeBoolean(included);
    }
}
