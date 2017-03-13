package simple;

public class ALittleOfEverything 
{
    private static class Foo
    {
        private final int i;
        public Foo() {
            i = 2;
        }
        public int getIt() {
            return i;
        }
    }
    
    public static void main(String[] args) 
    {
        Foo f = new Foo();
        Foo[] fs = new Foo[] {f};
        fs[0].getIt();
    }

    public ALittleOfEverything() {

        int rand = (int)Math.random();

        switch (rand) {

            case 0: break;
            case 1: 
            case 2: hashCode();
                break;

                default:
                break;
        }
    }

}
