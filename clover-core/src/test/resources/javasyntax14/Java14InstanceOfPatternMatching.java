public class Java14InstanceOfPatternMatching {
    public static void main(String[] args) {
        instanceOfCasting();
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
            System.out.println("obj is not null and is an Object and not String or Integer");
        }
    }
}
