/**
 * Regression for JEP 511 disambiguation (Java 25): an ordinary import whose first path
 * segment is literally the word 'module' ('module' is only a contextual keyword) must NOT
 * be mistaken for a module-import declaration. The grammar decides on the token after
 * 'module': an IDENT means a module import, a DOT means an ordinary dotted import.
 *
 * This fixture is instrumented (parsed) only - it is not compiled/run, since the imported
 * type is fictional; the point is purely that the parser accepts the ordinary import form.
 */
import module.something.Widget;

public class Java25OrdinaryImportModulePrefix {
    public static void main(String[] args) {
        Widget w = new Widget();
        System.out.println("widget = " + w);
    }
}
