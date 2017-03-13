package com.atlassian.samples.money;

/**
 * The common interface for simple Monies and MoneyBags
 */
public interface IMoney {
    /**
     * Adds a money to this money.
     */
    IMoney add(IMoney m);

    /**
     * Adds a simple Money to this money. This is a helper method for
     * implementing double dispatch
     */
    IMoney addMoney(Money m);

    /**
     * Adds a MoneyBag to this money. This is a helper method for
     * implementing double dispatch
     */
    IMoney addMoneyBag(MoneyBag s);

    /**
     * Tests whether this money is zero
     */
    boolean isZero();

    /**
     * Multiplies a money by the given factor.
     */
    IMoney multiply(int factor);

    /**
     * Negates this money.
     */
    IMoney negate();

    /**
     * Subtracts a money from this money.
     */
    IMoney subtract(IMoney m);

    /**
     * Append this to a MoneyBag m.
     */
    void appendTo(MoneyBag m);
}