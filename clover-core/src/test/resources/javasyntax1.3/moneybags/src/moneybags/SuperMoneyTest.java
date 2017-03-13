package moneybags;

import junit.framework.TestCase;

/**
 *
 */
public class SuperMoneyTest extends TestCase {
    protected Money f14CHF;
    protected IMoney fMB1;
    protected IMoney fMB2;
    protected Money f12CHF;
    protected Money f7USD;
    protected Money f21USD;

    protected void setUp() {
        f12CHF= new Money(12, "CHF");
        f14CHF= new Money(14, "CHF");
        f7USD= new Money( 7, "USD");
        f21USD= new Money(21, "USD");

        fMB1= MoneyBag.Mint.create(f12CHF, f7USD);
        fMB2= MoneyBag.Mint.create(f14CHF, f21USD);
    }

    protected void tearDown() throws Exception {
        // call toString, guranteed to be only called from here.
        // this is for testing, coverage by test.
        fMB1.toString();
    }


    class Inner {

    }

    public void testBagMultiply() {
        // {[12 CHF][7 USD]} *2 == {[24 CHF][14 USD]}
        IMoney expected= MoneyBag.Mint.create(new Money(24, "CHF"), new Money(14, "USD"));
        assertEquals(expected, fMB1.multiply(2));
        assertEquals(fMB1, fMB1.multiply(1));
        assertTrue(fMB1.multiply(0).isZero());
    }

    public void testBagNegate() {
        // {[12 CHF][7 USD]} negate == {[-12 CHF][-7 USD]}
        IMoney expected= MoneyBag.Mint.create(new Money(-12, "CHF"), new Money(-7, "USD"));
        assertEquals(expected, fMB1.negate());
    }

    public void testBagSimpleAdd() {
        // {[12 CHF][7 USD]} + [14 CHF] == {[26 CHF][7 USD]}
        IMoney expected= MoneyBag.Mint.create(new Money(26, "CHF"), new Money(7, "USD"));
        assertEquals(expected, fMB1.add(f14CHF));
    }

    public void testBagSubtract() {
        // {[12 CHF][7 USD]} - {[14 CHF][21 USD] == {[-2 CHF][-14 USD]}
        IMoney expected= MoneyBag.Mint.create(new Money(-2, "CHF"), new Money(-14, "USD"));
        assertEquals(expected, fMB1.subtract(fMB2));
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(SuperMoneyTest.class);
    }
}
