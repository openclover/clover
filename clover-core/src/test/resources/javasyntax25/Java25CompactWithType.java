// A compact compilation unit is a non-empty sequence of class member declarations
// (at least one of which is a method), optionally preceded by imports. It may
// freely mix methods, fields AND type declarations, just like a class body. This file is therefore
// unambiguously a COMPACT source file - it has a top-level method - even though it also contains a
// top-level 'class Box' declaration (which becomes a member class of the implicit class).
import java.util.ArrayList;

static String tag(String s) {
    return "<" + s + ">";
}

void main() {
    ArrayList<String> items = new ArrayList<>();
    items.add(tag("a"));
    items.add(tag("b"));
    Box box = new Box(items.size());
    System.out.println("count = " + box.count());
    System.out.println("first = " + items.get(0));
}

class Box {
    private final int n;

    Box(int n) {
        this.n = n;
    }

    int count() {
        return n;
    }
}
