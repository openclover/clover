package org.openclover.eclipse.test;

public class HelloWorld {

    sealed interface Shape permits Circle, Rectangle {}
    record Circle(double radius) implements Shape {}
    record Rectangle(double width, double height) implements Shape {}

    public static double area(Shape shape) {
        // instanceof pattern matching (standard in Java 16+, works in Java 17)
        if (shape instanceof Circle c) {
            return Math.PI * c.radius() * c.radius();
        } else if (shape instanceof Rectangle r) {
            return r.width() * r.height();
        }
        throw new IllegalArgumentException("Unknown shape: " + shape);
    }

    public static String greet(String name) {
        return """
               Hello, %s!
               """.formatted(name).strip();
    }
}
