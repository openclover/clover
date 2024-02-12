package sanity;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

class MattTest {

    // enum
    enum Color {
        red, green, blue
    }

    // varargs
    public static void printf(String fmt, Object[] args/*...*/) {
        int i = 0;
        // foreach on primitive array
        for (char c : fmt.toCharArray()) {
            if (c == '%')
                System.out.print(args[i++]);
            else
                System.out.print(c);
        }
        for (char z : fmt.toCharArray()) {
        }
    }

    public void foo1(List<char[]> a) {
        for (List<char[]> l = a; !l.isEmpty(); l = a) {
            char[] z = l.get(0);
            System.out.println(z);
        }

    }

    public boolean addAll(Collection<? extends Comparable> c) {
        boolean modified = false;
        Iterator<? extends Comparable> e = c.iterator();
        while (e.hasNext()) {
            if (add(e.next()))
                modified = true;
        }
        return modified;
    }

    public boolean add(Comparable o) {
        return true;
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

        for (List<String> ls = ys; !ls.isEmpty(); ls = ys)
            y = ls.get(0);

        // string list list
        LinkedList<LinkedList<String>> zss = new LinkedList<LinkedList<String>>();
        zss.add(ys);
        String z = zss.iterator().next().iterator().next();

        // foreach on a collection
        for (String s : ys)
            System.out.println(s);

        // varargs and boxing
//	printf("Addition: % plus % equals %\n", 1, 1, 2);
//
//	// use static import
//	printf("sin(PI/12) = %\n", sin(PI/12));
//
//	// use enums
//	printf("Colors are %\n", Color.VALUES);
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

        List<String> emptyStringList = Collections.<String>emptyList();
    }

}
