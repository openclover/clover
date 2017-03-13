package com.atlassian.samples.money;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * This is a trivial test which only tests the Money class.
 * If you modify the MoneyBag class, and run Clover with optimization, this test will not be run.
 */
public class MoneyTest {

    @Test
    public void testAdd() throws InterruptedException {
        Money tenEuro = new Money(10, "EURO");
        assertEquals(10, tenEuro.amount());
        assertEquals("EURO", tenEuro.currency());
        System.out.println("Tests taking too long? Try Clover's test optimization.");
        Thread.sleep(1000);
    }
    
}
