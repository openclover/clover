package typeannotation.newoperator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.HashMap;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class TypeAnnotationInNewOperator {
    private final HashMap<String, String> map = new @AnnotationForType1 HashMap<>();
}

@Retention(RUNTIME) @Target({ElementType.TYPE_USE})
@interface AnnotationForType1 {
}
