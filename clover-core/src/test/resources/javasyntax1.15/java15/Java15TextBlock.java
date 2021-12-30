package java15;

public class Java15TextBlock {
    public void textBlock() {

        System.out.println("""
                """);

        System.out.println("""
                "" " " ""
                """);

        System.out.println("""
                \" \"" \u005c" \u005c""
                """);

        System.out.println("""
                \t \r \n \r \t
                """);

        System.out.println("""
                \u005c\ \\u005c
                """);

        System.out.println("""
                text \
                concatinated""");

        System.out.println("""
                contains "" \u005c""
                """);

        System.out.println("""
                contains \""" \u005c"""
                """);
    }
}
