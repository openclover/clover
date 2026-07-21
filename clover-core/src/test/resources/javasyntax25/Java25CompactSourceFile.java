/// JEP 512 - Compact Source Files and Instance Main Methods (finalized in Java 25).
/// The whole compilation unit is a stream of top-level members with no enclosing class
/// declaration, and main() is a non-static instance method with no String[] parameter.
/// Clover models the top-level members under a synthetic class named after the file base
/// name (javac compiles this file to a class literally called Java25CompactSourceFile).

String greeting = "Hello";

int doubled(int n) {
    return n * 2;
}

void main() {
    System.out.println("greeting = " + greeting);
    System.out.println("doubled = " + doubled(21));
}
