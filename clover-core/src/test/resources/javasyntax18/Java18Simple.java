/**
 * A minimal Java 18 source file. Java 18 introduced no permanent language syntax
 * (JEP 420 Pattern Matching for switch was only a 2nd preview), so this file only
 * proves that Clover accepts '--source 18' and instruments an ordinary class.
 */
public class Java18Simple {
    public static void main(String[] args) {
        int sum = add(2, 3);
        System.out.println("Java18Simple sum = " + sum);
    }

    static int add(int a, int b) {
        return a + b;
    }
}
