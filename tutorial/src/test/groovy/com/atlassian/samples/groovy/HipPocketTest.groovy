package com.atlassian.samples.groovy;

import org.junit.Test

import static org.junit.Assert.assertTrue;

class HipPocketTest {

    @Test
    void testEmptyOnCreate() {
        HipPocket pocket = new HipPocket()
        assertTrue pocket.isEmpty()
    }

    @Test
    void testInsert() {
        HipPocket pocket = new HipPocket()
        assertTrue pocket.isEmpty()
        pocket.insert(2, "EUR")
        assertTrue !pocket.isEmpty()
    }
}

