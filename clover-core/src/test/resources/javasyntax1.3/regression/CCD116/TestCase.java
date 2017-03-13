// package statement prevents reproduction of the bug!
//package regression.CCD116;

/**
 * see http://equity:8080/secure/ViewIssue.jspa?key=CCD-116
 */
public class TestCase
{
    private Class type;



    private void a(Object o)
    {
        if(type == null) type = o.getClass();
        // can't have any non-assignment "expression" statements before this method, or the bug doesn't occur
        else if(type!=o.getClass()) throw new IllegalArgumentException();
    }

    public void b() {

    }
}
