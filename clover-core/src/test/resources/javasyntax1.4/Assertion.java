/**
 * Tests new Java 1.4 language feature - assertions.
 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/language/assert.html
 */
public class Assertion {
    /**
     * Tests "assert Expression1;"
     */
    public int simpleAssert(int i) {
        assert i > 0;
        i++;
        return i;
    }

    /**
     * Tests "assert Expression1:Expression2;"
     */
    public int assertWithMessage(int i) {
        assert i >= 0 : "Input value must be positive";
        i--;
        return i;
    }
}