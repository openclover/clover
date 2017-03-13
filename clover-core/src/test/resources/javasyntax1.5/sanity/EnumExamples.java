package sanity;
//

// okay first some generics tests
interface Intf1<A extends Comparable> {}
interface Intf2<A extends java.awt.Component> {}
interface Intf3<A extends java.awt.Component & Runnable> {}
interface Intf4<A extends java.awt.Component & Runnable & Comparable> {}

// now some enum decls
enum Enum1 {CLUBS, DIAMONDS, HEARTS, SPADES};
class TestEnum1 
{
    void testit(Enum1 x) 
    {
        switch (x) {
        case CLUBS:
            System.out.println("clubs");
        }
    }
    
}

enum Enum2 {CLUBS, DIAMONDS, HEARTS, SPADES,};
enum Enum3 {
    CLUBS, DIAMONDS, HEARTS, SPADES;
    void fooMethod() {}
};
enum Enum4 {
    CLUBS, DIAMONDS, HEARTS, SPADES,;
    void fooMethod() {}
};

enum Enum5 {
    CLUBS("c"), DIAMONDS("h"), HEARTS("d"), SPADES("s");
    private final String otherName;
    Enum5(String x) {
        otherName = x;
    }
    public int foo() {return 1;}
};
enum Enum6 {
    CLUBS("c"),
    DIAMONDS("h") {public int foo() {return 3;}},
    HEARTS {public int foo() {return 4;}},
    SPADES;
    private final String otherName;
    Enum6(String x) {
        otherName = x;
    }
    Enum6() {
        otherName = "noname";
    }
    public int foo() {return 1;}
};

interface EnumInter1 {}
interface EnumInter2<T> {}

enum Enum7 implements EnumInter1 {A, B}
enum Enum8 implements EnumInter1, EnumInter2<String>, java.util.Comparator<String> {
    A, B;
    public int compare(String o1, String o2) {
        return o1.compareTo(o2);
    }
}


class EnhancedForTest 
{
    public void foo() {
        int[] a = {1,2};
        
        for(final int i: a) {
            System.out.println(i);
        }
    }
}

enum EnumWithSemicolon { ; }
enum EnumWithCommaAndSemmi { ,; }
enum EnumWithConstantAndComma { A ,; }
enum EnumWithoutConstants {
    ;
    private EnumWithoutConstants() {
    }
}
