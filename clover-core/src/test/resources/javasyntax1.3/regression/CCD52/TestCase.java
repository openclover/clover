package regression.CCD52;

/**
 * see http://equity:8080/secure/ViewIssue.jspa?key=CCD-52
 */
public class TestCase
{
    public static void test1() {
        Runnable r = new Runnable() { 
            public void run() { 
                final class Derived { 
                    public Derived() { 
                        super();
                        // the problem in CCD-52 is the above line, instr was being
                        // inserted BEFORE the super call (but only when the anon class
                        // definition was part of a nested statement (declaration?)
                        try { 
                            System.out.println("Hello"); 
                        } finally { 
                            System.out.println("At Last"); 
                        } 
                    } 
                } 
            } 
        };
    }

    public static void test2() {
        Runnable r = new Runnable() { 
            public void run() { 
                final class Derived { 
                    public Derived() { 
                        this(1);
                        // test calls to "this"
                        System.out.println("a");
                    } 
                    public Derived(int i) { 
                        this(1, i);
                        System.out.println("b");
                    } 
                    public Derived(int i, int j) { 
                        System.out.println("c");
                    } 
                } 
            } 
        };
    }

    
}
