package org.openclover.idea.testproject.subpackage;

public class M {
    int a;
    int b;
    boolean flag;

    public void m1() {
        a++;
        if (flag) {
            b++;
        }
    }

    public void m2() {
        b++;
    }
}
