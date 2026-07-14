package org.openclover.idea.build;

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
     * Defensive equals() delegating to {@link #equalsTo(ValidityState)} so that OpenClover validity
     * states compare by value regardless of how the platform invokes equality checks.
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
