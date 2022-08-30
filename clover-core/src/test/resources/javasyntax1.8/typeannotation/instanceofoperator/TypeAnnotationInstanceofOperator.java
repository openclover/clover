package typeannotation.instanceofoperator;

import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

public class TypeAnnotationInstanceofOperator {
    public void typeAnnotation(Object obj) {
        if (obj instanceof @AnnotationForType1 String) {
            System.out.println(obj);
        }
    }
}

@Retention(RUNTIME) @Target({ElementType.TYPE_USE})
@interface AnnotationForType1 {
}
