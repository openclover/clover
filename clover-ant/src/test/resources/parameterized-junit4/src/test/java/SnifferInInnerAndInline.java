/**
 * Test instrumentation when we have static and non-static innner test classes or anonymous test inline classes
 */
public class SnifferInInnerAndInline {
    interface TestAction {
        boolean test();
    }

    static class InnerStaticTest {
        public void testInnerStatic() {

        }
    }

    class InnerTest {
        public void testInnerNonStatic() {

        }
    }

    TestAction testWithInline() {
        return new TestAction() {
            public boolean test() {
                return true;
            }
        };
    }

    public static void main(String[] args) {
        new SnifferInInnerAndInline.InnerStaticTest().testInnerStatic();
        new SnifferInInnerAndInline().new InnerTest().testInnerNonStatic();
        new SnifferInInnerAndInline().testWithInline().test();
    }

}
