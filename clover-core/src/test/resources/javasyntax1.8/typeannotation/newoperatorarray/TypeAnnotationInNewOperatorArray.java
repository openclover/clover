package typeannotation.newoperatorarray;

import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

public class TypeAnnotationInNewOperatorArray {
    private int [][] intArray = new int @AnnotationForType1 [2][2];
}

@Retention(RUNTIME) @Target({ElementType.TYPE_USE})
@interface AnnotationForType1 {
}
