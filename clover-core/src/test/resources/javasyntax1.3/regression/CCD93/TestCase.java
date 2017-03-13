package regression.CCD93;

/**
 * 
 */
public class TestCase
{

    public static void main(String[] args) {
        // watch the "german umlaut" !!
        char[] c = new char[] { 'ä', 'ö', 'ß' };
        for ( int n = 0; n < c.length; n++ ) {
            switch ( c[n] ) {
                case 'ä': System.out.println("ae"); break;
                case 'ö': System.out.println("oe"); break;
                case 'ß': System.out.println("sz"); break;
            }
        }
    }

}
