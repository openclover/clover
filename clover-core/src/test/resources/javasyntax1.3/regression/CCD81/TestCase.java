package regression.CCD81;


public class TestCase
{

    public static int returnZero() {
        for ( ; true; ) {
            return 0;
        }
    }

    public static void main(String[] args) {
         int zero = returnZero();
    }
}