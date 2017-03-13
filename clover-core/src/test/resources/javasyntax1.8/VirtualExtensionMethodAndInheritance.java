/**
 * JEP126 - http://openjdk.java.net/jeps/126
 */
public class VirtualExtensionMethodAndInheritance {
    static interface A {
        default void helloWorld() {
            System.out.println("Hello World from A");
        }
    }

    static interface B extends A {
        default void helloWorld() {
            System.out.println("Hello World from B");
        }
    }

    public static class C implements A, B {
        public static void main(String... args) {
            new C().helloWorld(); // B is the most specific type
        }
    }

    public static void main(String[] args) {
        C.main();
    }
}
