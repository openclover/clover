/*
 */
package regression.CCD361;



/**
   this is a Sun Javac bug. see
   http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6481655

*/
import java.util.Collections;

public class ParenExprWithExplicitType {

  public static void main(String [] args) {
      assert Collections.emptyList().size() == 0;
      assert (Collections.emptyList().size() == 0);

     /* no point this continually failing
      // compile failure here with normal compilation
      assert (Collections.<String>emptyList().size() == 0);

      // compile failure here after instrumentation
      assert Collections.<String>emptyList().size() == 0;
    */
  }
}