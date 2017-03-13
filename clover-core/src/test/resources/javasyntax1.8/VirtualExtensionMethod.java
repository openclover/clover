import java.util.Iterator;

/**
 * Sample how we can use 'default' keyword.
 *
 * JEP126 - http://openjdk.java.net/jeps/126
 */
public interface VirtualExtensionMethod extends Iterator {
    /** Should be instrumented */
    default boolean isLast() {
        return !hasNext();
    }

    /** Test "default" in a method signature vs "default" in a case statement */
    public default void chooseLast(int option) {
        switch (option) {
            case 1: break;
            case 2:
            case 3:
            default:
                isLast();
        }

        switch (option) {
            case 0: break;
            case 1:
            case 2: hashCode();
                break;
            default:
                break;
        }
    }

    /** Non-instrumentable as empty */
    void forwardToLast();
}
