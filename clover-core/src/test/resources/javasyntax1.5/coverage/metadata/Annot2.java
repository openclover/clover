package coverage.metadata;

public @interface Annot2 {
    String value();

    public static int zero = 0;

    public class InnerClass
    {
        public void foo() {
            System.out.println("Annot2.InnerClass.foo");
        }
    }

    public enum InnerEnum {
        RED, GREEN, BLUE;
        public void foo() {
            System.out.println("Annot2.InnerEnum.foo");
        }
    }

}
