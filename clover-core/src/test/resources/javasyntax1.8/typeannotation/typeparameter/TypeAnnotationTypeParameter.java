package typeannotation.typeparameter;

import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

public class TypeAnnotationTypeParameter<@AnnotationForType1 T> {
    public void bar(String obj) {
    }
}

@Retention(RUNTIME) @Target({ElementType.TYPE_USE})
@interface AnnotationForType1 {
}
