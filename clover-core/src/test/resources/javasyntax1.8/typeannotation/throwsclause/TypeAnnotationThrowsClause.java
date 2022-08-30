package typeannotation.throwsclause;

import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

public class TypeAnnotationThrowsClause {
    public void foo() throws @AnnotationForType1 Exception {
        try {
            throw new Exception();
        } finally {
        }
    }
}

@Retention(RUNTIME) @Target({ElementType.TYPE_USE})
@interface AnnotationForType1 {
}
