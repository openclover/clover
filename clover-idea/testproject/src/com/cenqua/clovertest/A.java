package com.cenqua.clovertest;

import java.util.Date;

public class A {
    int a;
    int b;

    public void a1() {
        a++;
        a++;
        a++;
        a++;
        a++;
    }

    public void a2() {
        b++;
        b++;
    }

    static boolean method(Date date) {
        boolean inRange = false;
        if (date != null) {
            inRange = date.getTime() > new Date(2000, 1, 1).getTime();

        }
        return !inRange;
    }
}