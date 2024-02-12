package typeannotation.typeparameter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class TypeAnnotationTypeParameter<@AnnotationForType1 T> {
    public void bar(String obj) {
    }
}

@Retention(RUNTIME) @Target({ElementType.TYPE_USE})
@interface AnnotationForType1 {
}
