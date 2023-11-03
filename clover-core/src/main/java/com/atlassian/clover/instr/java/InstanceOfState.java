package com.atlassian.clover.instr.java;

/**
 * A state machine for analysis of instanceof expressions.
 * Can be used to determine if the instanceof expression contains variable declaration
 * (i.e. instanceof pattern matching), which cannot be branch-instrumented.
 *
 * <pre>
 * NOTHING -> INSTANCEOF -> TYPE -> COMPLEX_TYPE -> VARIABLE
 *                             |                     ^
 *                             +---------------------+
 * </pre>
 */
public enum InstanceOfState {
    NOTHING() {
        @Override
        InstanceOfState nextToken(CloverToken token) {
            // "o instanceof"
            return token.getType() == JavaTokenTypes.INSTANCEOF
                    ? INSTANCEOF
                    : NOTHING;
        }
    },

    INSTANCEOF() {
        @Override
        InstanceOfState nextToken(CloverToken token) {
            // "o instanceof A"
            return token.getType() == JavaTokenTypes.IDENT
                    ? FULL_TYPE
                    : NOTHING;
        }
    },

    FULL_TYPE() {
        @Override
        InstanceOfState nextToken(CloverToken token) {
            if (token.getType() == JavaTokenTypes.DOT
                    || token.getType() == JavaTokenTypes.LBRACK) {
                // "o instanceof A." or "o instanceof A[" etc
                return PARTIAL_TYPE;
            } else if (token.getType() == JavaTokenTypes.IDENT) {
                // "o instanceof A a"
                return VARIABLE;
            } else {
                return NOTHING;
            }
        }
    },

    PARTIAL_TYPE() {
        @Override
        InstanceOfState nextToken(CloverToken token) {
            if (token.getType() == JavaTokenTypes.RBRACK) {
                // "o instanceof A[]"
                return FULL_TYPE;
            } else if (token.getType() == JavaTokenTypes.IDENT) {
                // "o instanceof A.B" or "o instanceof A.B.C" etc
                return FULL_TYPE;
            } else {
                return NOTHING;
            }
        }
    },

    VARIABLE() {
        @Override
        InstanceOfState nextToken(CloverToken token) {
            // we don't care about further tokens, lock on this state
            return VARIABLE;
        }
    };

    abstract InstanceOfState nextToken(CloverToken token);
}
