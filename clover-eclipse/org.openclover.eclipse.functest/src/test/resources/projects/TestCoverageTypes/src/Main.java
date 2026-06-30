
public class Main {
	public static void methodCoveredIncidentally() {
		System.out.println("INCIDENTAL COVERAGE");
	}
	
	public static void methodCoveredByPassingTest() {
		System.out.println("TEST COVERAGE");
	}
	
	public static void methodCoveredByFailingTest() {
		System.out.println("FAILING TEST COVERAGE");
	}

	public static void methodCoveredByFailingAndPassingTest() {
		System.out.println("FAILING AND PASSING TEST COVERAGE");
	}
}