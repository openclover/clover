package typeannotation.fielddeclarationpostposingarray;

import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

public class TypeAnnotationInFieldDeclarationPostposingArray {
    private int intArray @AnnotationForType1 [][] = new int[2][2];
}

@Retention(RUNTIME) @Target({ElementType.TYPE_USE})
@interface AnnotationForType1 {
}
