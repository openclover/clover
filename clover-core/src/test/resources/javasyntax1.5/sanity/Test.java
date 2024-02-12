package sanity;

import java.util.Collections;
import java.util.LinkedList;

import static java.lang.Math.PI;
import static java.lang.Math.sin;

class Test {

    // enum
    enum Color {
        red, green, blue
    }

    // varargs
    public static void printf(String fmt, Object... args) {
        int i = 0;
        // foreach on primitive array
        for (char c : fmt.toCharArray()) {
            if (c == '%')
                System.out.print(args[i++]);
            else if (c == '\n')
                System.out.println();
            else
                System.out.print(c);
        }
    }

    public static void main(String[] args) {
        // Integer list
        LinkedList<Integer> xs = new LinkedList<Integer>();
        xs.add(new Integer(0));
        xs.add(new Integer(1));
        Integer x = xs.iterator().next();
        Integer mb = Collections.max(xs);

        // string list
        LinkedList<String> ys = new LinkedList<String>();
        ys.add("zero");
        ys.add("one");
        String y = ys.iterator().next();

        // string list list
        LinkedList<LinkedList<String>> zss = new LinkedList<LinkedList<String>>();
        zss.add(ys);
        String z = zss.iterator().next().iterator().next();

        // foreach on a collection
        for (String s : ys)
            System.out.println(s);

        // varargs and boxing
        printf("Addition: % plus % equals %\n", 1, 1, 2);

        // use static import
        printf("sin(PI/12) = %\n", sin(PI / 12));

        // use enums
        printf("Colors are % % %\n", (Object[]) Color.values());
        for (Color c : Color.values()) {
            // switch on enum
            switch (c) {
                case red:
                    System.out.println("found red.");
                    break;
                case green:
                    System.out.println("found green.");
                    break;
                case blue:
                    System.out.println("found blue.");
                    break;
            }
        }
    }
}
