/**
 * JEP 513 - Flexible Constructor Bodies (finalized in Java 25).
 * Statements are allowed in a constructor's "prologue" - before the explicit this(...) /
 * super(...) invocation - as long as they don't reference the instance under construction.
 * Clover must instrument the constructor entry exactly once and the result must still compile.
 */
public class Java25FlexibleConstructor {

    static class Person {
        final String name;
        final int age;

        // prologue statement before super()
        Person(String name, int age) {
            if (name == null) {
                throw new IllegalArgumentException("name required");
            }
            super();
            this.name = name;
            this.age = age;
        }

        // prologue statement before this(...)
        Person(String name) {
            int defaultAge = 30;
            this(name, defaultAge);
        }
    }

    public static void main(String[] args) {
        Person p1 = new Person("Alice", 42);
        Person p2 = new Person("Bob");
        System.out.println("p1 = " + p1.name + " " + p1.age);
        System.out.println("p2 = " + p2.name + " " + p2.age);
    }
}
