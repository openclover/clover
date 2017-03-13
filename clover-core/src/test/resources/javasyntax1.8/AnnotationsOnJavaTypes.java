import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Annotations on Java Types. This Java8 enhancement allows to put annotations in a number of new places,
 * as presented below.
 *
 * JSR308 - http://jcp.org/en/jsr/detail?id=308
 * JEP104 - http://openjdk.java.net/jeps/104
 * specification - http://types.cs.washington.edu/jsr308/specification/java-annotation-design.pdf
 */
public class AnnotationsOnJavaTypes {
// TODO IT SEEMS THAT JDK1.8 M7 DOES NOT SUPPORT THESE ANNOTATIONS YET

    static @interface Readonly { }

    static @interface NonNull { }

    static @interface NonEmpty { }

    // annotations for method receivers
    public int size() /*@Readonly*/ {
        return 0;
    }

    // annotations inside generic type arguments
    Map</*@NonNull*/ String, /*@NonEmpty*/ List</*@Readonly*/ Document>> files;

    // annotations inside arrays
    // docs1 is an unmodifiable one-dimensional array of mutable Documents
    Document[/*@Readonly*/] docs1;
    // docs2 is a mutable array whose elements are unmodifiable one-dimensional arrays of mutable Documents
    Document[][/*@Readonly*/] docs2 = new Document[2][/*@Readonly*/ 12];

    public void annotationInTypeCast(Object myObject) {
        String myString;
        // annotation in typecast
        myString = (/*@NonNull*/ String)myObject;
    }

    public void annotationInInstanceOf(Object myString) {
        // type tests
        boolean isNonNull = myString instanceof /*@NonNull*/ String;
    }

    public void annotationInObjectCreation() {
        Set myNonEmptyStringSet = new HashSet();
        // object creation
        new /*@Readonly @NonEmpty*/ ArrayList(myNonEmptyStringSet);
    }


    //type parameter bounds:
    //class Folder { ... }
    // ???

    // class inheritance
    abstract class UnmodifiableList<T> implements /*@Readonly*/ List</*@Readonly*/ T> { }

    // throws clauses
    class TemperatureException extends Exception { }
    @interface Critical { }
    void monitorTemperature() throws /*@Critical*/ TemperatureException {
        throw new TemperatureException();
    }
}

