package typeannotation.fielddeclarationgenerics;

import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.HashMap;

/* Testcase for singleTypeArgument */
public class TypeAnnotationInFieldDeclarationGenerics<T> {
    private final HashMap<@AnnotationForType1 String, String> map1 = new HashMap<>();
    private final HashMap<@AnnotationForType1 @AnnotationForType2 String, String> map2 = new HashMap<>();
    private final HashMap<String, @AnnotationForType1 int[]> map3 = new HashMap<>();
    private final HashMap<String, @AnnotationForType1 @AnnotationForType2 T> map4 = new HashMap<>();
    private final HashMap<String, @AnnotationForType1 ?> map5 = new HashMap<>();
    private final HashMap<String, @AnnotationForType1 @AnnotationForType2 ?> map6 = new HashMap<>();
    private final HashMap<String, @AnnotationForType1 ? extends @AnnotationForType1 int[]> map7 = new HashMap<>();
    private final HashMap<String, @AnnotationForType1 @AnnotationForType2 ? super @AnnotationForType1 @AnnotationForType2 T> map8 = new HashMap<>();

    public static void main(String args[]) {
        TypeAnnotationInFieldDeclarationGenerics<?> j = new TypeAnnotationInFieldDeclarationGenerics<Object>();
        j.getSize();
    }
    public void getSize() {
        System.out.println(map1.size());
        System.out.println(map2.size());
        System.out.println(map3.size());
        System.out.println(map4.size());
        System.out.println(map5.size());
        System.out.println(map6.size());
        System.out.println(map7.size());
        System.out.println(map8.size());
    }
}

@Retention(RUNTIME) @Target({ElementType.TYPE_USE})
@interface AnnotationForType1 {
}

@Retention(RUNTIME) @Target({ElementType.TYPE_USE})
@interface AnnotationForType2 {
}
