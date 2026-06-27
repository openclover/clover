package org.openclover.eclipse.test;

import java.util.List;
import java.util.stream.Collectors;

public class HelloWorld {

    public static List<String> greet(List<String> names) {
        return names.stream()
                .filter(n -> !n.isEmpty())
                .map(n -> "Hello, " + n + "!")
                .collect(Collectors.toList());
    }
}
