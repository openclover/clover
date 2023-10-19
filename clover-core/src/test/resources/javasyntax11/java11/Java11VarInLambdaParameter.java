package java11;

import java.util.function.Consumer;

public class Java11VarInLambdaParameter {
    public void varInLambdaParameter() {
        var function1 = (Consumer<String>) (var testString) -> {
            if (testString.isEmpty()) {
                System.out.println("string is empty");
            }
        };
        function1.accept("");
    }
}
