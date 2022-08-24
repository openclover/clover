package typeannotation.fielddeclarationgenerics;

import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.HashMap;

public class TypeAnnotationInFieldDeclarationGenerics {
    private final HashMap<@AnnotationForType1 String, String> map = new HashMap<>();

    public static void main(String args[]) {
        TypeAnnotationInFieldDeclarationGenerics j = new TypeAnnotationInFieldDeclarationGenerics();
        j.getSize();
    }
    public void getSize() {
        System.out.println(map.size());
    }
}

@Retention(RUNTIME) @Target({ElementType.TYPE_USE})
@interface AnnotationForType1 {
}
