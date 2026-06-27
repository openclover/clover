package org.openclover.eclipse.test;

public class HelloWorld {

    record Point(int x, int y) {}

    public static String describe(Object obj) {
        return switch (obj) {
            case Integer i -> "int:" + i;
            case String s  -> "str:" + s;
            case Point p   -> "point:(" + p.x() + "," + p.y() + ")";
            case null      -> "null";
            default        -> "other";
        };
    }
}
