import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class MainTest {
	@Before
	public void setUp() {
		Main.methodCoveredIncidentally();
	}

	@Test
	public void testPassingCoverage() {
		Main.methodCoveredByPassingTest();
		Main.methodCoveredByFailingAndPassingTest();
	}

	@Test
	public void testFailingCoverage() {
		Main.methodCoveredByFailingTest();
		Main.methodCoveredByFailingAndPassingTest();
		fail();
	}
}
