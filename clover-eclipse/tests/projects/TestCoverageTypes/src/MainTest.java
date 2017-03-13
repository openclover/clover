import java.util.Collection;

import junit.framework.TestCase;

public class MainTest extends TestCase {
	public void setUp() {
		Main.methodCoveredIncidentally();
	}
	
	public void testPassingCoverage() {
		Main.methodCoveredByPassingTest();
		Main.methodCoveredByFailingAndPassingTest();
	}
	
	public void testFailingCoverage() {
		Main.methodCoveredByFailingTest();
		Main.methodCoveredByFailingAndPassingTest();
		fail();
	}
}