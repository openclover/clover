package regression.CCD424;

/**
 *
 */

public enum Foo implements java.io.Serializable {
    STRING_CMP(new Comparable() {
        public int compareTo(Object o) {
            return 0;
        }
    });

    Comparable comp;

    Foo(Comparable c) {
        comp = c;
    }
}