package regression.CLOV158;



public @interface DefaultAnnotation {

public boolean enabled() default true;
public String[] groups() default {};
public String description() default "";





}