package com.atlassian.clover.instr.java;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public class RewriteLambdaToBlockMatcher {

    private static final Set<String> TO_REWRITE_CALLEE_METHODS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
            "min",
            "map",
            "generate",
            "iterate",
            "reduce",
            "max",
            "concat",
            "peek",
            "filter",
            "flatMap",
            "collect",
            "transform",
            "compose",
            "from",
            "transformValues",
            "thenApply",
            "thenCompose"
    )));

    public static boolean shouldRewriteAsBlock(Deque<Deque<String>> currentStack) {
        ArrayDeque<Deque<String>> stack = new ArrayDeque<>(currentStack);
        return !stack.isEmpty() && isCalleeMethodOnStack(stack);
    }

    private static boolean isCalleeMethodOnStack(ArrayDeque<Deque<String>> stack) {
        stack.pop(); // additional empty stack since Lambda is expression and creates new stack

        if (stack.isEmpty()) {
            return false; // it's lambda expression like Runnable run = () -> sth();
        }

        final Deque<String> lambdaCallStack = stack.pop();

        if (lambdaCallStack.isEmpty()) {
            return false; // it's lambda expression like Callable<Runnable> run = () -> () -> sth();
        }
        final String callee = lambdaCallStack.pop();

        return TO_REWRITE_CALLEE_METHODS.contains(callee);
    }
}
