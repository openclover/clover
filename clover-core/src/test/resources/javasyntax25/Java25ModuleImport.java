/**
 * JEP 511 - Module Import Declarations (finalized in Java 25).
 * 'import module M;' imports all packages exported by module M. Here java.base gives us
 * java.util.* etc. without an explicit single-type import. 'module' is only a contextual
 * keyword. Not instrumented; the fixture just proves the new import form parses.
 */
import module java.base;

public class Java25ModuleImport {
    public static void main(String[] args) {
        List<String> items = new ArrayList<>();
        items.add("a");
        items.add("b");
        System.out.println("size = " + items.size());
    }
}
