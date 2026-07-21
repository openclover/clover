package org.openclover.core.instr.java;

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
 *                     |                 ^     ^ | \
 *                     +---- FINAL ------+     | |  \------------(  )--> RECORD_DECONSTRUCTION
 *                                             | +----> PARTIAL_TYPE
 *                                             |          |
 *                                             +----------+
 * </pre>
 *
 * The VARIABLE and RECORD_DECONSTRUCTION end states both mean the instanceof introduces a pattern
 * binding (a type pattern variable or a record deconstruction pattern), which must not be
 * branch-instrumented.
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
            // "o instanceof A" or "o instanceof int[]" (primitive array type)
            return token.getType() == JavaTokenTypes.IDENT || isPrimitiveType(token)
                    ? FULL_TYPE
                    : NOTHING;
        }
    };

    final static InstanceOfState FINAL = new InstanceOfState() {
        @Override
        InstanceOfState nextToken(CloverToken token) {
            // "o instanceof final A" or "o instanceof final int[]" (primitive array type)
            return token.getType() == JavaTokenTypes.IDENT || isPrimitiveType(token)
                    ? FULL_TYPE
                    : NOTHING;
        }
    };

    /**
     * Whether the token is a primitive type keyword. A primitive type can be used in instanceof
     * pattern matching only as an array component, e.g. "o instanceof int[] arr".
     */
    private static boolean isPrimitiveType(CloverToken token) {
        switch (token.getType()) {
            case JavaTokenTypes.BOOLEAN:
            case JavaTokenTypes.BYTE:
            case JavaTokenTypes.CHAR:
            case JavaTokenTypes.SHORT:
            case JavaTokenTypes.INT:
            case JavaTokenTypes.LONG:
            case JavaTokenTypes.FLOAT:
            case JavaTokenTypes.DOUBLE:
                return true;
            default:
                return false;
        }
    }

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
            } else if (token.getType() == JavaTokenTypes.LPAREN) {
                // "o instanceof A(...)" - the '(' begins a record deconstruction pattern (JEP 440)
                return RECORD_DECONSTRUCTION;
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

    /**
     * A record deconstruction pattern, e.g. "o instanceof Point(int x, int y)" (JEP 440). Like a
     * pattern variable, the bound components are variable declarations, so an instanceof expression
     * containing a record pattern must not be branch-instrumented. We don't care about the tokens
     * that follow (nested patterns, an optional trailing binding name), so we lock on this state.
     */
    final static InstanceOfState RECORD_DECONSTRUCTION = new InstanceOfState() {
        @Override
        InstanceOfState nextToken(CloverToken token) {
            return RECORD_DECONSTRUCTION;
        }
    };


    private InstanceOfState() {

    }
    abstract InstanceOfState nextToken(CloverToken token);
}
