/// JEP 467 - Markdown Documentation Comments (finalized in Java 23).
/// This class is documented with `///` Markdown doc comments instead of `/** */`.
///
/// - a bullet point
/// - another bullet
///
/// The Clover lexer treats `///` as an ordinary line comment and discards it, so these
/// comments must have no effect on instrumentation.
public class Java23MarkdownDoc {

    /// Adds two integers.
    /// @param a first addend
    /// @param b second addend
    /// @return the sum
    static int add(int a, int b) {
        return a + b;
    }

    public static void main(String[] args) {
        System.out.println("sum = " + add(2, 3));
    }
}
