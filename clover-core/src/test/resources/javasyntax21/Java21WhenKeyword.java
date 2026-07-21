/**
 * Regression: 'when' is only a contextual keyword (used in guarded patterns). It must
 * still be usable as an ordinary identifier - field, method and variable name - so that
 * pre-existing code keeps compiling and instrumenting. Compare with 'record' in Java 16.
 */
public class Java21WhenKeyword {
    int when = 0;

    int when(int when) {
        this.when = when;
        System.out.println("when = " + when);
        return when;
    }

    public static void main(String[] args) {
        Java21WhenKeyword obj = new Java21WhenKeyword();
        int when = obj.when(10);
        System.out.println("result = " + when);
    }
}
