package org.openclover.core.instr.java

import spock.lang.Specification
import spock.lang.Unroll

class RewriteLambdaToBlockMatcherTest extends Specification {


    def "when empty stack rewrite as a block should be false"() {
        setup:
        Deque<Deque<String>> stack = new ArrayDeque<>()

        when:
        boolean result = RewriteLambdaToBlockMatcher.shouldRewriteAsBlock(stack)

        then:
        !result
    }

    def "when empty stack has only one substack rewrite as a block should be false"() {
        setup:
        Deque<Deque<String>> stack = new ArrayDeque<>()
        stack.push(new ArrayDeque<String>())

        when:
        boolean result = RewriteLambdaToBlockMatcher.shouldRewriteAsBlock(stack)

        then:
        !result
    }

    def "when no identifiers on stack rewrite as a block should be false"() {
        setup:
        Deque<Deque<String>> stack = new ArrayDeque<>()
        stack.push(new ArrayDeque<String>())
        stack.push(new ArrayDeque<String>())

        when:
        boolean result = RewriteLambdaToBlockMatcher.shouldRewriteAsBlock(stack)

        then:
        !result
    }

    @Unroll
    def "when last method on stack is #methodName rewrite as a block should be #expectedHeuristic"() {
        setup:
        boolean result = RewriteLambdaToBlockMatcher.shouldRewriteAsBlock(stack)

        expect:
        result == expectedHeuristic


        where:
        methodName         | stack                                   | expectedHeuristic
        "test"             | stackWithLastMethod("test")             | false
        "Test"             | stackWithLastMethod("Test")             | false
        "test1"            | stackWithLastMethod("test1")            | false
        "otherMethodAtAll" | stackWithLastMethod("otherMethodAtAll") | false
        "andSomethingElse" | stackWithLastMethod("andSomethingElse") | false
        "sequential"       | stackWithLastMethod("sequential")       | false
        "distinct"         | stackWithLastMethod("distinct")         | false
        "skip"             | stackWithLastMethod("skip")             | false
        "findFirst"        | stackWithLastMethod("findFirst")        | false
        "empty"            | stackWithLastMethod("empty")            | false
        "findAny"          | stackWithLastMethod("findAny")          | false
        "of"               | stackWithLastMethod("of")               | false
        "limit"            | stackWithLastMethod("limit")            | false
        "toArray"          | stackWithLastMethod("toArray")          | false
        "builder"          | stackWithLastMethod("builder")          | false
        "close"            | stackWithLastMethod("close")            | false
        "unordered"        | stackWithLastMethod("unordered")        | false
        "count"            | stackWithLastMethod("count")            | false
        "sorted"           | stackWithLastMethod("sorted")           | false
        "onClose"          | stackWithLastMethod("onClose")          | false
        "allMatch"         | stackWithLastMethod("allMatch")         | false
        "iterator"         | stackWithLastMethod("iterator")         | false
        "mapToInt"         | stackWithLastMethod("mapToInt")         | false
        "flatMapToInt"     | stackWithLastMethod("flatMapToInt")     | false
        "flatMapToLong"    | stackWithLastMethod("flatMapToLong")    | false
        "mapToLong"        | stackWithLastMethod("mapToLong")        | false
        "parallel"         | stackWithLastMethod("parallel")         | false
        "noneMatch"        | stackWithLastMethod("noneMatch")        | false
        "isParallel"       | stackWithLastMethod("isParallel")       | false
        "forEachOrdered"   | stackWithLastMethod("forEachOrdered")   | false
        "anyMatch"         | stackWithLastMethod("anyMatch")         | false
        "mapToDouble"      | stackWithLastMethod("mapToDouble")      | false
        "spliterator"      | stackWithLastMethod("spliterator")      | false
        "flatMapToDouble"  | stackWithLastMethod("flatMapToDouble")  | false
        "forEach"          | stackWithLastMethod("forEach")          | false
        "map"              | stackWithLastMethod("map")              | true
        "min"              | stackWithLastMethod("min")              | true
        "generate"         | stackWithLastMethod("generate")         | true
        "iterate"          | stackWithLastMethod("iterate")          | true
        "reduce"           | stackWithLastMethod("reduce")           | true
        "max"              | stackWithLastMethod("max")              | true
        "concat"           | stackWithLastMethod("concat")           | true
        "peek"             | stackWithLastMethod("peek")             | true
        "filter"           | stackWithLastMethod("filter")           | true
        "flatMap"          | stackWithLastMethod("flatMap")          | true
        "collect"          | stackWithLastMethod("collect")          | true
        "compose"          | stackWithLastMethod("compose")          | true
        "from"             | stackWithLastMethod("from")             | true
        "transformValues"  | stackWithLastMethod("transformValues")  | true
        "thenApply"        | stackWithLastMethod("thenApply")        | true
        "thenCompose"      | stackWithLastMethod("thenCompose")      | true
    }

    def stackWithLastMethod(String methodName) {
        Deque<Deque<String>> stack = new ArrayDeque<>()
        ArrayDeque<String> firstStack = new ArrayDeque<>()

        firstStack.push(methodName)

        stack.push(firstStack)
        stack.push(new ArrayDeque<String>())
        stack
    }
}
