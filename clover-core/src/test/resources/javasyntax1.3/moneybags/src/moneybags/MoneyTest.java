package moneybags;

public class MoneyTest extends SuperMoneyTest {

//    public List<Boolean> testGenerics() {
//        LinkedList<Boolean> list = new LinkedList<Boolean>(new HashSet<Boolean>());
//        list.add(Boolean.TRUE);
//        for (Boolean aBoolean : list) {
//            list.set(list.indexOf(aBoolean), new Boolean(!aBoolean.booleanValue()));
//        }
//        return list;
//    }


    public static void main(String args[]) {
        junit.textui.TestRunner.run(MoneyTest.class);
    }

    public void testBagSumAdd() {
        // {[12 CHF][7 USD]} + {[14 CHF][21 USD]} == {[26 CHF][28 USD]}
        IMoney expected= MoneyBag.Mint.create(new Money(26, "CHF"), new Money(28, "USD"));
        assertEquals(expected, fMB1.add(fMB2));
    }
    public void testIsZero() {
        assertTrue(fMB1.subtract(fMB1).isZero());
        assertTrue(MoneyBag.Mint.create(new Money (0, "CHF"), new Money (0, "USD")).isZero());
    }
    public void testMixedSimpleAdd() {
        // [12 CHF] + [7 USD] == {[12 CHF][7 USD]}
        IMoney expected= MoneyBag.Mint.create(f12CHF, f7USD);
        assertEquals(expected, f12CHF.add(f7USD));
    }
    public void testBagNotEquals() {
        IMoney bag= MoneyBag.Mint.create(f12CHF, f7USD);
        assertFalse(bag.equals(new Money(12, "DEM").add(f7USD)));
    }
    public void testMoneyBagEquals() {
        assertTrue(!fMB1.equals(null));

        assertEquals(fMB1, fMB1);
        IMoney equal= MoneyBag.Mint.create(new Money(12, "CHF"), new Money(7, "USD"));
        assertTrue(fMB1.equals(equal));
        assertTrue(!fMB1.equals(f12CHF));
        assertTrue(!f12CHF.equals(fMB1));
        assertTrue(!fMB1.equals(fMB2));
    }
    public void testMoneyBagHash() {
        IMoney equal= MoneyBag.Mint.create(new Money(12, "CHF"), new Money(7, "USD"));
        assertEquals(fMB1.hashCode(), equal.hashCode());
    }
    public void testMoneyEquals() {
        assertTrue(!f12CHF.equals(null));
        Money equalMoney= new Money(12, "CHF");
        assertEquals(f12CHF, f12CHF);
        assertEquals(f12CHF, equalMoney);
        assertEquals(f12CHF.hashCode(), equalMoney.hashCode());
        assertTrue(!f12CHF.equals(f14CHF));
    }
    public void testMoneyHash() {
        assertTrue(!f12CHF.equals(null));
        Money equal= new Money(12, "CHF");
        assertEquals(f12CHF.hashCode(), equal.hashCode());
    }
    public void testSimplify() {
        IMoney money= MoneyBag.Mint.create(new Money(26, "CHF"), new Money(28, "CHF"));
        assertEquals(new Money(54, "CHF"), money);
    }
    public void testNormalize2() {
        // {[12 CHF][7 USD]} - [12 CHF] == [7 USD]
        Money expected= new Money(7, "USD");
        assertEquals(expected, fMB1.subtract(f12CHF));
    }
    public void testNormalize3() {
        // {[12 CHF][7 USD]} - {[12 CHF][3 USD]} == [4 USD]
        IMoney ms1= MoneyBag.Mint.create(new Money(12, "CHF"), new Money(3, "USD"));
        Money expected= new Money(4, "USD");
        assertEquals(expected, fMB1.subtract(ms1));
    }
    public void testNormalize4() {
        // [12 CHF] - {[12 CHF][3 USD]} == [-3 USD]
        IMoney ms1= MoneyBag.Mint.create(new Money(12, "CHF"), new Money(3, "USD"));
        Money expected= new Money(-3, "USD");
        assertEquals(expected, f12CHF.subtract(ms1));
    }
    public void testPrint() {
        assertEquals("[12 CHF]", f12CHF.toString());
    }
    public void testSimpleAdd() {
        // [12 CHF] + [14 CHF] == [26 CHF]
        Money expected= new Money(26, "CHF");
        assertEquals(expected, f12CHF.add(f14CHF));
    }
    public void testSimpleBagAdd() {
        // [14 CHF] + {[12 CHF][7 USD]} == {[26 CHF][7 USD]}
        IMoney expected= MoneyBag.Mint.create(new Money(26, "CHF"), new Money(7, "USD"));
        assertEquals(expected, f14CHF.add(fMB1));
    }
    public void testSimpleMultiply() {
        // [14 CHF] *2 == [28 CHF]
        Money expected= new Money(28, "CHF");
        assertEquals(expected, f14CHF.multiply(2));
    }
    public void testSimpleNegate() {
        // [14 CHF] negate == [-14 CHF]
        Money expected= new Money(-14, "CHF");
        assertEquals(expected, f14CHF.negate());
    }
    public void testSimpleSubtract() {
        // [14 CHF] - [12 CHF] == [2 CHF]
        Money expected= new Money(2, "CHF");
        assertEquals(expected, f14CHF.subtract(f12CHF));
    }

    public void testReallyReallyReallyReallyReallyReallyReallyReallyLongMethodName() {
        Money expected= new Money(2, "CHF");
        assertEquals(expected, f14CHF.subtract(f12CHF));
    }

    /**
     * see CCD-389 for the feature being tested here.
     */
    public void testThisTestShouldFail() {
        Money expected = new Money(4, "CHF");
        assertEquals(expected, f12CHF.divide(3));
        // now fail deliberately.
        assertEquals(expected, f12CHF.divide(0));
    }

    public void testLinkificationInReport() {
        new MoneyBag.Mint();
    }

    public void testSerialization() throws java.io.IOException {
        IMoney bag = MoneyBag.Mint.create(new Money(1, "CHF"), new Money(2, "CHF"));
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);
        try {
            oos.writeObject(bag);
        } catch (java.io.IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());
        java.io.ObjectInputStream ois = new java.io.ObjectInputStream(bais);
        try {
            IMoney bag2 = (IMoney) ois.readObject();
            assertEquals(bag, bag2);
        } catch (ClassNotFoundException e) {
            fail(e.getMessage());
        }

    }

    public String testTestsWithReturnTypes() {
        return "Testing instrumentation of tests with return clause.";
    }

    static {
        String hi = "hi";
    }    
}