package org.openclover.idea.testproject.subpackage;

import junit.framework.TestCase;
import org.openclover.idea.testproject.A;

public class MTest extends TestCase {

    public void testM() {
        new M().m1();
    }

    public void testMM() {
        M m = new M();
        m.m1();
        m.m2();
    }

    public void testAM() {
        M m = new M();
        m.m1();
        m.m2();

        new A().a1();
    }

}
