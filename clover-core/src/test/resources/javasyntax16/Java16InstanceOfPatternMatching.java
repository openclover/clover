
public class Java16InstanceOfPatternMatching {
    public static void main(String[] args) {
        instanceOfCasting();
        instanceOfInExpressions();
    }

    private static void instanceOfCasting() {
        Object obj = "a string";
        if (obj instanceof String str) {
            System.out.println("obj is String = " + str);
        } else {
            System.out.println("obj is Object = " + obj);
        }
    }

    private static void instanceOfInExpressions() {
        Object obj = new Object();
        if (obj != null && obj instanceof Object && !(obj instanceof String str || obj instanceof Integer i)) {
            // note: str and i are not accessible as they're in a branch condition!
            System.out.println("obj is not null and is an Object and not String or Integer");
        }
    }
}
