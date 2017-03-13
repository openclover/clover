package regression.CCD350;

public @interface MyAnnotation{
public enum Mode { IN, OUT, INOUT };

public Mode mode() default Mode.IN;
}