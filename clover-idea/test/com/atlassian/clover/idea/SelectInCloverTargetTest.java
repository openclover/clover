package com.atlassian.clover.idea;

import com.intellij.ide.SelectInContext;
import junit.framework.TestCase;

/**
 * SelectInCloverTarget Tester.
 */
public class SelectInCloverTargetTest extends TestCase {
    public void testBucket() {
        MockView m1 = new MockView();
        MockView m2 = new MockView();
        MockView m3 = new MockView();

        SelectInCloverTarget target = new SelectInCloverTarget();
        target.addView(m1, 1);
        target.addView(m2, 1);
        target.addView(m3, 1);

        target.selectIn(null, false);
        assertTrue(m1.wasCalled());
        assertTrue(m2.wasCalled());
        assertTrue(m3.wasCalled());
    }

    public void testPriority() {
        MockView m1 = new MockView();
        MockView m2 = new MockView();
        MockView m3 = new MockView();

        SelectInCloverTarget target = new SelectInCloverTarget();
        target.addView(m1, 1);
        target.addView(m2, 2);
        target.addView(m3, 3);

        target.selectIn(null, false);
        assertTrue(m1.wasCalled());
        assertTrue(m2.wasCalled());
        assertTrue(m3.wasCalled());

        assertTrue(m2.selectedAt > m1.selectedAt);
        assertTrue(m3.selectedAt > m2.selectedAt);
    }

    public void testConsume() {
        MockView m1 = new MockView();
        MockView m2 = new MockView(true);
        MockView m3 = new MockView();

        SelectInCloverTarget target = new SelectInCloverTarget();
        target.addView(m1, 1);
        target.addView(m2, 2);
        target.addView(m3, 3);

        target.selectIn(null, false);
        assertTrue(m1.wasCalled());
        assertTrue(m2.wasCalled());
        assertFalse(m3.wasCalled());

        assertTrue(m2.selectedAt > m1.selectedAt);
    }

    public void testRemove() {
        MockView m1 = new MockView();
        MockView m2 = new MockView();
        MockView m3 = new MockView();

        SelectInCloverTarget target = new SelectInCloverTarget();
        target.addView(m1, 1);
        target.addView(m2, 2);
        target.addView(m3, 3);

        target.removeView(m2);

        target.selectIn(null, false);
        assertTrue(m1.wasCalled());
        assertFalse(m2.wasCalled());
        assertTrue(m3.wasCalled());

        assertTrue(m3.selectedAt > m1.selectedAt);
    }


    public void testCanSelect() {
        MockView m1 = new MockView();
        MockView m2 = new MockView();
        MockView m3 = new MockView();

        SelectInCloverTarget target = new SelectInCloverTarget();
        target.addView(m1, 1);
        target.addView(m2, 2);
        target.addView(m3, 3);

        assertFalse(target.canSelect(null));
        m3.canSelect = true;
        assertTrue(target.canSelect(null));

    }

}

class MockView implements SelectInCloverView {
    boolean canSelect;
    boolean consume;

    MockView() {
        this(false);
    }

    MockView(boolean consume) {
        this.consume = consume;
    }

    static int selectCounter;
    int selectedAt = -1;

    @Override
    public boolean canSelect(SelectInContext context) {
        return canSelect;
    }

    @Override
    public boolean selectIn(SelectInContext context) {
        //noinspection AssignmentToStaticFieldFromInstanceMethod
        selectedAt = ++selectCounter;
        return consume;
    }

    public boolean wasCalled() {
        return selectedAt != -1;
    }
}