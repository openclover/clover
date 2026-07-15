package org.openclover.tutorial.money;

/**
 * The common interface for simple Monies and MoneyBags
 */
public interface IMoney {
    /**
     * Adds money to this money.
     */
    IMoney add(IMoney m);

    /**
     * Adds simple Money to this money. This is a helper method for
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
     * Multiplies money by the given factor.
     */
    IMoney multiply(int factor);

    /**
     * Negates this money.
     */
    IMoney negate();

    /**
     * Subtracts money from this money.
     */
    IMoney subtract(IMoney m);

    /**
     * Append this to a MoneyBag m.
     */
    void appendTo(MoneyBag m);
}