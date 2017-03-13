package regression;


public class CLOV243 {

    public CLOV243() {
    }

    class Inner {
        public Inner() {
        }
    }

    CLOV243 x = null;
    class Foo extends CLOV243.Inner {
        public Foo(Bar bar) {
            // uncomment for build fail:
            // bar.getTest().super(); // instr error occurs here
        }
    }

    class Bar {
        public CLOV243 getTest() {
            return new CLOV243();
        }

    }

}

