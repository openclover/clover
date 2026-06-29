/** UTF-8 encoded source — tests that Clover instruments files with non-ASCII characters.
 *  Greets in several languages: Héllo, Wörld, Ñoño, こんにちは */
public class HelloWorldUtf8 {
    public static String greet() {
        return "Héllo, Wörld!";
    }
}
