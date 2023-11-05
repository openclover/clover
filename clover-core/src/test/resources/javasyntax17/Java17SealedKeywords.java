public class Java17SealedKeywords {
    // not allowed by java 17
    // class sealed { }
    // interface sealed { }

    public static void main(String[] args) {
        int sealed = 10;
        int non = 20;
        int permits = 30;
        // FAIL: int total = non-sealed + permits;
        // TODO the "non-sealed" is treated as NON_SEALED token instead of arithmetic operation
        // FAIL: int total = non- sealed + permits;
        // TODO the "non-" is recognized as a beginning of "non-sealed" and expects "s" instead of " "
        int total = non - sealed + permits;
        System.out.println("total=" + total);

        sealed();
        permits();
    }

    static void sealed() {
        System.out.println("called sealed()");
    }

    static void permits() {
        System.out.println("called permits()");
    }

    int non = 0;
    int sealed = 100;
    int permits = 200;

    {
        non = 0;
        sealed = 101;
        permits = 202;

        System.out.println("sealed=" + sealed + " permits=" + permits);
    }
}
