package org.openclover.eclipse.test;

public class HelloWorld {

    sealed interface Shape permits Circle, Rectangle {}
    record Circle(double radius) implements Shape {}
    record Rectangle(double width, double height) implements Shape {}

    public static double area(Shape shape) {
        return switch (shape) {
            case Circle c    -> Math.PI * c.radius() * c.radius();
            case Rectangle r -> r.width() * r.height();
        };
    }

    public static String greet(String name) {
        return """
               Hello, %s!
               """.formatted(name).strip();
    }
}
