import java.io.IOException;

/**
 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/language/catch-multiple.html
 */
public class MultipleCatch {
    static class FirstException extends IOException { }
    static class SecondException extends IOException { }
    static class ThirdException extends Exception { }

    /**
     * Classic approach. Catch exceptions in separate catch clauses.
     */
    public void catchExceptionClassic(String exceptionName) throws IOException {
        try {
            if (exceptionName.equals("First")) {
                throw new FirstException();
            } else {
                throw new SecondException();
            }
        } catch (FirstException e) {
            throw e;
        } catch (SecondException e) {
            throw e;
        }
    }

    /**
     * New approach. Group exceptions in one catch clause.
     */
    public void catchExceptionNew(String exceptionName) throws IOException {
        try {
            if (exceptionName.equals("First")) {
                throw new FirstException();
            } else {
                throw new SecondException();
            }
        } catch (FirstException|SecondException e) {
            throw e;
        }
    }

    /**
     * Classic approach. You cannot declare more specific types in throws clause than
     * declared in catch() block.
     */
    public void rethrowExceptionClassic(String exceptionName) throws IOException {
        try {
            if (exceptionName.equals("First")) {
                throw new FirstException();
            } else {
                throw new SecondException();
            }
        } catch (IOException e) {
            // actually we catch either FirstException or SecondException
            throw e;
        }
    }

    /**
     * New approach. We can declare more specific types in throws clause, but still
     * have one catch block with more general exception.
     */
    public void rethrowExceptionNew(String exceptionName)
            throws FirstException, SecondException // we know from code analysis that only FirstException or SecondException is rethrown
    {
        try {
            if (exceptionName.equals("First")) {
                throw new FirstException();
            } else {
                throw new SecondException();
            }
        } catch (IOException e) {
            // we declare IOException caught, but
            // actually we catch either FirstException or SecondException
            throw e;
        }
    }

}