package org.openclover.eclipse.test;

import java.util.List;
import java.util.stream.Collectors;

public class HelloWorld {

    public static String greet(String name) {
        var trimmed = name.strip();
        return "Hello, " + trimmed + "!";
    }

    public static List<String> filterNonBlanks(List<String> lines) {
        return lines.stream()
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }
}
