package coverage.metadata;

public class TestCases {
    public static void main(String... args) {

        Annot2.InnerClass o1 = new Annot2.InnerClass();
        o1.foo();

        Annot2.InnerEnum.RED.foo();

        DeprecatedTest.foo();
    }
}
