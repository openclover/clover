/**
 * A minimal Java 19 source file. Java 19 introduced no finalized language syntax
 * (Record Patterns and Pattern Matching for switch were still previews), so this file
 * only proves that Clover accepts '--source 19' and instruments an ordinary class.
 */
public class Java19Simple {
    public static void main(String[] args) {
        int sum = add(2, 3);
        System.out.println("Java19Simple sum = " + sum);
    }

    static int add(int a, int b) {
        return a + b;
    }
}
