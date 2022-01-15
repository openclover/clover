package typeannotation.implementsclause;

import static java.lang.annotation.RetentionPolicy.*;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

public class TypeAnnotationImplements implements @AnnotationForType1 Serializable {
}

@Retention(RUNTIME) @Target({ElementType.TYPE_USE})
@interface AnnotationForType1 {
}
