import org.testng.annotations.Test;

@Test
public class TestNGJDK15TestCase {

	@Test
	public void noExceptionEncountered() {

	}

	@Test(expectedExceptions = {MyException.class})
	public void expectedExceptionNotEncountered1() throws MyException {

	}

	@Test(expectedExceptions = {MyException.class})
	public void expectedExceptionNotEncountered2() {

	}

	@Test(expectedExceptions = {MyException.class})
	public void expectedExceptionEncountered1() throws MyException {
		throw new MyException(method("expectedExceptionEncountered1"));
	}

	@Test(expectedExceptions = {MyException.class})
	public void expectedExceptionEncountered2() throws Exception {
		throw new MyException(method("expectedExceptionEncountered2"));
	}

	@org.testng.annotations.Test(expectedExceptions = {MyException.class})
	public void expectedExceptionEncountered3() throws MyException {
		throw new MyException(method("expectedExceptionEncountered3"));
	}

	@org.testng.annotations.Test @org.testng.annotations.ExpectedExceptions(MyException.class)
	public void expectedExceptionEncountered4() throws MyException {
		throw new MyException(method("expectedExceptionEncountered4"));
	}

	@Test(expectedExceptions = {MyException.class})
	public void unexpectedRuntimeException1() throws MyException {
		throw new RuntimeException(method("unexpectedRuntimeException1"));
	}

	@Test(expectedExceptions = {MyException.class})
	public void unexpectedRuntimeException2() throws MyException {
		throw new RuntimeException(method("unexpectedRuntimeException2"));
	}

	@Test
	public void unexpectedCheckedException() {
		TypeWhichChangesThrowsBehaviourAfterCompilation.throwCheckedException(method("unexpectedCheckedException"));
	}

	@Test(expectedExceptions = {Exception.class})
	public void undeclaredExpectedCheckedException() {
		TypeWhichChangesThrowsBehaviourAfterCompilation.throwCheckedException(method("undeclaredExpectedCheckedException"));
	}

	@Test(expectedExceptions = {RuntimeException.class})
	public void expectedRuntimeException() {
		throw new RuntimeException(method("expectedRuntimeException"));
	}

	@Test(expectedExceptions = {Error.class})
	public void expectedError() {
		throw new Error(method("expectedError"));
	}

	public String method(String name) {
		return "Exception generated from test method: " + name;
	}
}

