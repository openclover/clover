package coverage.enums;

public enum E2
{
    RED(),
    GREEN() {
        public void foo() {
            System.out.println("GREEN foo called");
        }
    },
    BLUE()
    ;

    public void foo() {
        System.out.println("base foo called");
    }
    
}
