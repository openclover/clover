package typeannotation.fielddeclarationarray;

import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

public class TypeAnnotationInFieldDeclarationArray {
    private int @AnnotationForType1 [][] intArray = new int[2][2];
}

@Retention(RUNTIME) @Target({ElementType.TYPE_USE})
@interface AnnotationForType1 {
}
