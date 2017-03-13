package coverage.enums;

public class EnumTests 
{
    public static void main(String[] args) {
        E1 e1 = E1.RED;
        System.out.println("e1.RED.num=" + e1.getNum());
        
        E2.RED.foo();
        E2.GREEN.foo();
        E2.BLUE.foo();

        E3.GREEN.toString();
    }
}
