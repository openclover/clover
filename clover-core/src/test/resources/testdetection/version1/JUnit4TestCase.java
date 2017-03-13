import org.junit.Test;

public class JUnit4TestCase {
	@Test
	public void noExceptionEncountered() {

	}

	@Test(expected = MyException.class)
	public void expectedExceptionNotEncountered1() throws MyException {
	}

	@Test(expected = MyException.class)
	public void expectedExceptionNotEncountered2() {

	}

	@Test(expected = MyException.class)
	public void expectedExceptionEncountered1() throws MyException {
		throw new MyException(method("expectedExceptionEncountered1"));
	}

	@Test(expected = MyException.class)
	public void expectedExceptionEncountered2() throws Exception {
		throw new MyException(method("expectedExceptionEncountered2"));
	}

	@org.junit.Test(expected = MyException.class)
	public void expectedExceptionEncountered3() throws MyException {
		throw new MyException(method("expectedExceptionEncountered3"));
	}

	@org.junit.Test(expected = Exception.class)
	public void expectedExceptionEncountered4() throws Exception {
		throw new MyException(method("expectedExceptionEncountered4"));
	}

	@Test(expected = MyException.class)
	public void unexpectedRuntimeException1() throws MyException {
		throw new RuntimeException(method("unexpectedRuntimeException1"));
	}

	@Test(expected = MyException.class)
	public void unexpectedRuntimeException2() throws MyException {
		throw new RuntimeException(method("unexpectedRuntimeException2"));
	}

	@Test
	public void unexpectedCheckedException() {
		TypeWhichChangesThrowsBehaviourAfterCompilation.throwCheckedException(method("unexpectedCheckedException"));
	}

	@Test(expected = Exception.class)
	public void undeclaredExpectedCheckedException() {
		TypeWhichChangesThrowsBehaviourAfterCompilation.throwCheckedException(method("undeclaredExpectedCheckedException"));
	}

	@Test(expected = RuntimeException.class)
	public void expectedRuntimeException() {
		throw new RuntimeException(method("expectedRuntimeException"));
	}

	@Test(expected = Error.class)
	public void expectedError() {
		throw new Error(method("expectedError"));
	}
	
	public String method(String name) {
		return "Exception generated from test method: " + name;
	}
}
