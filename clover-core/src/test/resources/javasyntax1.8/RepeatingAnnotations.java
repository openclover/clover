import java.lang.annotation.Repeatable;

/**
 * Change the Java programming language to allow multiple application of annotations with the same type to a single
 * program element.
 *
 * JEP120 - http://openjdk.java.net/jeps/120
 */
public class RepeatingAnnotations {

    // Note: Clover is not able to support containers of repeated annotations, i.e. we cannot see the @Bits associated
    // with a foo(). Java reflections as well as annotation processing tools can see this. Clover sees only three
    // @Bit annotations as defined on the source code level.

    // a container class for holding multiple annotations of the same type
    public @interface Bits {
        Bit[] value();
    }

    // the @Repeatable annotation that allows to declare @Bit many times
    @Repeatable(Bits.class)
    @interface Bit {
        int value();
    }

    // now we can have multiple annotations of the same type
    @Bit(1)
    @Bit(2)
    @Bit(3)
    void foo() {

    }

}
