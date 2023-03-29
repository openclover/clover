/**
 * @testng.test
 */
public class TestNGJDK14TestCase {

	/**
	 * @testng.test
	 */
	public void noExceptionEncountered() {

	}

	/**
	 * @testng.test expectedExceptions = "MyException"
	 */
	public void expectedExceptionNotEncountered1() throws MyException {

	}

	/**
	 * @testng.test expectedExceptions = "MyException"
	 */
	public void expectedExceptionNotEncountered2() {

	}

	/**
	 * @testng.test expectedExceptions = "MyException"
	 */
	public void expectedExceptionEncountered1() throws MyException {
		throw new MyException(method("expectedExceptionEncountered1"));
	}

	/**
	 * @testng.test expectedExceptions = "MyException"
	 */
	public void expectedExceptionEncountered2() throws Exception {
		throw new MyException(method("expectedExceptionEncountered2"));
	}

	/**
	 * @testng.test
	 * @testng.expected-exceptions value = "MyException"
	 */
	public void expectedExceptionEncountered3() throws MyException {
		throw new MyException(method("expectedExceptionEncountered3"));
	}

	/**
	 * this is redundant - its here just for symmetry with the JDK 1.5 tests so that they
	 * can all be tested in the same way. 
	 *
	 * @testng.test
	 * @testng.expected-exceptions value = "MyException"
	 */
	public void expectedExceptionEncountered4() throws MyException {
		throw new MyException(method("expectedExceptionEncountered4"));
	}

	/**
	 * @testng.test expectedExceptions = "MyException"
	 */
	public void unexpectedRuntimeException1() throws MyException {
		throw new RuntimeException(method("unexpectedRuntimeException1"));
	}

	/**
	 * @testng.test expectedExceptions = "MyException"
	 */
	public void unexpectedRuntimeException2() throws MyException {
		throw new RuntimeException(method("unexpectedRuntimeException2"));
	}

	/**
	 * @testng.test
	 */
	public void unexpectedCheckedException() {
		TypeWhichChangesThrowsBehaviourAfterCompilation.throwCheckedException(method("unexpectedCheckedException"));
	}

	/**
	 * @testng.test expectedExceptions = "java.lang.Exception"
	 */
	public void undeclaredExpectedCheckedException() {
		TypeWhichChangesThrowsBehaviourAfterCompilation.throwCheckedException(method("undeclaredExpectedCheckedException"));
	}

	/**
	 * @testng.test expectedExceptions = "java.lang.RuntimeException"
	 */
	public void expectedRuntimeException() {
		throw new RuntimeException(method("expectedRuntimeException"));
	}

	/**
	 * @testng.test expectedExceptions = "java.lang.Error"
	 */
	public void expectedError() {
		throw new Error(method("expectedError"));
	}

	public String method(String name) {
		return "Exception generated from test method: " + name;
	}
}
