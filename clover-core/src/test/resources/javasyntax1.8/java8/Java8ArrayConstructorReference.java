package java8;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Java8ArrayConstructorReference {
    public static byte[][] toPayloads(String... strings) {
        return Arrays.stream(strings).map(s -> s == null ? null : s.getBytes(StandardCharsets.UTF_8))
                .toArray(byte[][]::new);
    }
}
