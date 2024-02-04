package com.atlassian.clover.instr.java;

/**
 * A state machine for analysis of instanceof expressions.
 * Can be used to determine if the instanceof expression contains variable declaration
 * (i.e. instanceof pattern matching), which cannot be branch-instrumented.
 *
 * <pre>
 *                                             +------+    +--+
 *                                             |      |    v  |
 *                                             | +----GENERIC_TYPE(n)
 *                                             v |
 * NOTHING -> INSTANCEOF --------------> FULL_TYPE --------------------> VARIABLE
 *                     |                 ^     ^ |
 *                     +---- FINAL ------+     | +----> PARTIAL_TYPE
 *                                             |          |
 *                                             +----------+
 * </pre>
 *
 * Why it's a class and not an enum? Because we need GENERIC_TYPE with variable 'depth' attribute, see below.
 */
public abstract class InstanceOfState {

    private static class GenericTypeInstanceOfState extends InstanceOfState {
        private final int depth;

        private GenericTypeInstanceOfState(int depth) {
            this.depth = depth;
        }

        @Override
        InstanceOfState nextToken(CloverToken token) {
            if (token.getType() == JavaTokenTypes.LT) {
                return GENERIC_TYPE(depth + 1);
            } else if (token.getType() == JavaTokenTypes.GT) { // '>'
                return depth == 1 ? FULL_TYPE : GENERIC_TYPE(depth - 1);
            } else if (token.getType() == JavaTokenTypes.SR) { // '>>'
                return depth == 2 ? FULL_TYPE : GENERIC_TYPE(depth - 2);
            } else if (token.getType() == JavaTokenTypes.BSR) { // '>>>'
                return depth == 3 ? FULL_TYPE : GENERIC_TYPE(depth - 3);
            } else {
                return this;
            }
        }
    }

    final static InstanceOfState NOTHING = new InstanceOfState() {
        @Override
        InstanceOfState nextToken(CloverToken token) {
            // "o instanceof"
            return token.getType() == JavaTokenTypes.INSTANCEOF
                    ? INSTANCEOF
                    : NOTHING;
        }
    };

    final static InstanceOfState INSTANCEOF = new InstanceOfState() {
        @Override
        InstanceOfState nextToken(CloverToken token) {
            // "o instanceof final"
            if (token.getType() == JavaTokenTypes.FINAL) {
                return FINAL;
            }
            // "o instanceof A"
            return token.getType() == JavaTokenTypes.IDENT
                    ? FULL_TYPE
                    : NOTHING;
        }
    };

    final static InstanceOfState FINAL = new InstanceOfState() {
        @Override
        InstanceOfState nextToken(CloverToken token) {
            // "o instanceof final A"
            return token.getType() == JavaTokenTypes.IDENT
                    ? FULL_TYPE
                    : NOTHING;
        }
    };

    final static InstanceOfState FULL_TYPE = new InstanceOfState() {
        @Override
        InstanceOfState nextToken(CloverToken token) {
            if (token.getType() == JavaTokenTypes.DOT
                    || token.getType() == JavaTokenTypes.LBRACK) {
                // "o instanceof (final) A." or "o instanceof (final) A[" etc
                return PARTIAL_TYPE;
            } else if (token.getType() == JavaTokenTypes.LT) {
                // "o instanceof (final) A<"
                return GENERIC_TYPE(1);
            } else if (token.getType() == JavaTokenTypes.IDENT) {
                // "o instanceof (final) A a"
                return VARIABLE;
            } else {
                return NOTHING;
            }
        }
    };

    /**
     * A generic type with "&lt;...&gt;". For simplicity, we accept all tokens inside triangular brackets.
     * Instead of this, we count &lt; and &gt; to know the depth and know when the declaration ends.
     */
    static InstanceOfState GENERIC_TYPE(int depth) {
        return new GenericTypeInstanceOfState(depth);
    };

    final static InstanceOfState PARTIAL_TYPE = new InstanceOfState() {
        @Override
        InstanceOfState nextToken(CloverToken token) {
            if (token.getType() == JavaTokenTypes.RBRACK) {
                // "o instanceof (final) A[]"
                return FULL_TYPE;
            } else if (token.getType() == JavaTokenTypes.IDENT) {
                // "o instanceof (final) A.B" or "o instanceof (final) A.B.C" etc
                return FULL_TYPE;
            } else {
                return NOTHING;
            }
        }
    };

    final static InstanceOfState VARIABLE = new InstanceOfState() {
        @Override
        InstanceOfState nextToken(CloverToken token) {
            // we don't care about further tokens, lock on this state
            return VARIABLE;
        }
    };


    private InstanceOfState() {

    }
    abstract InstanceOfState nextToken(CloverToken token);
}
