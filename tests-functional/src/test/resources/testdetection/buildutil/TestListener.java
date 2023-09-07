public class TestListener extends org.testng.TestListenerAdapter {
	@Override
	public void onTestFailure(org.testng.ITestResult tr) {
		System.out.print(tr.getName() + " - failed.");
		if (tr.getThrowable() != null) {
			System.out.print(" Exception: " + tr.getThrowable().getClass().getName() + " Message: \"" + tr.getThrowable().getMessage() + "\"");
		}
		System.out.println();
	}

	@Override
	public void onTestSkipped(org.testng.ITestResult tr) {
		System.out.println(tr.getName() + " - skipped.");
	}

	@Override
	public void onTestSuccess(org.testng.ITestResult tr) {
		System.out.println(tr.getName() + " - succeeded.");
	}
}
