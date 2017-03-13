package com.atlassian.samples.money;

import java.util.Vector;

/**
 * A MoneyBag defers exchange rate conversions. For example adding 12 Swiss Francs to 14 US Dollars is represented as a
 * bag containing the two Monies 12 CHF and 14 USD. Adding another 10 Swiss francs gives a bag with 22 CHF and 14 USD.
 * Due to the deferred exchange rate conversion we can later value a MoneyBag with different exchange rates. <p/> A
 * MoneyBag is represented as a list of Monies and provides different constructors to create a MoneyBag.
 */
class MoneyBag implements IMoney {
    private Vector<IMoney> fMonies = new Vector<IMoney>(5);

    static IMoney create(IMoney m1, IMoney m2) {
        MoneyBag result = new MoneyBag();
        m1.appendTo(result);
        m2.appendTo(result);
        return result.simplify();
    }

    public IMoney add(IMoney m) {
        return m.addMoneyBag(this);
    }

    public IMoney addMoney(Money m) {
        return MoneyBag.create(m, this);
    }

    public IMoney addMoneyBag(MoneyBag s) {
        return MoneyBag.create(s, this);
    }

    void appendBag(MoneyBag aBag) {
        for (IMoney fMony : aBag.fMonies) {
            appendMoney((Money) fMony);
        }
    }

    void appendMoney(Money aMoney) {
        if (aMoney.isZero()) return;
        IMoney old = findMoney(aMoney.currency());
        if (old == null) {
            fMonies.addElement(aMoney);
            return;
        }
        fMonies.removeElement(old);
        IMoney sum = old.add(aMoney);
        if (sum.isZero()) {
            return;
        }
        fMonies.addElement(sum);
    }

    /**
     * Equals allows to compare two MoneyBag objects
     */
    public boolean equals(Object anObject) {
        if (isZero()) {
            if (anObject instanceof IMoney) {
                return ((IMoney) anObject).isZero();
            }
        }

        if (anObject instanceof MoneyBag) {
            MoneyBag aMoneyBag = (MoneyBag) anObject;
            if (aMoneyBag.fMonies.size() != fMonies.size()) {
                return false;
            }

            for (IMoney fMony : fMonies) {
                Money m = (Money) fMony;
                if (!aMoneyBag.contains(m)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private Money findMoney(String currency) {
        for (IMoney fMony : fMonies) {
            Money m = (Money) fMony;
            if (m.currency().equals(currency)) {
                return m;
            }
        }
        return null;
    }

    private boolean contains(Money m) {
        Money found = findMoney(m.currency());
        return found != null && found.amount() == m.amount();
    }

    public int hashCode() {
        int hash = 0;
        for (IMoney m : fMonies) {
            hash ^= m.hashCode();
        }
        return hash;
    }

    public boolean isZero() {
        return fMonies.size() == 0;
    }

    public IMoney multiply(int factor) {
        MoneyBag result = new MoneyBag();
        if (factor != 0) {
            for (IMoney fMony : fMonies) {
                Money m = (Money) fMony;
                result.appendMoney((Money) m.multiply(factor));
            }
        }
        return result;
    }

    public IMoney negate() {
        MoneyBag result = new MoneyBag();
        for (IMoney fMony : fMonies) {
            Money m = (Money) fMony;
            result.appendMoney((Money) m.negate());
        }
        return result;
    }

    private IMoney simplify() {
        if (fMonies.size() == 1) {
            return fMonies.elements().nextElement();
        }
        return this;
    }

    public IMoney subtract(IMoney m) {
        return add(m.negate());
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("{");
        for (IMoney fMony : fMonies) {
            buffer.append(fMony);
        }
        buffer.append("}");
        return buffer.toString();
    }

    public void appendTo(MoneyBag m) {
        m.appendBag(this);
    }
}