package typeannotation.newoperatorarray;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class TypeAnnotationInNewOperatorArray {
    private int [][] intArray = new int @AnnotationForType1 [2][2];
}

@Retention(RUNTIME) @Target({ElementType.TYPE_USE})
@interface AnnotationForType1 {
}
