import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class PersonTest {


    @ParameterizedTest
    @MethodSource   ("getParameters")
    public void testPersonGender(String name, String expectedGender) {
        assertEquals(expectedGender, new Person(name).getGender());
    }

    static Collection<String[]> getParameters() {
        Collection<String[]> argStream = Arrays.asList(new String[][]{new String[]{"Ms. Jane Doe", Person.FEMALE},
                new String[]{"Mr. Alex Taylor", Person.MALE}, new String[]{"John Doe", Person.UNKNOWN}});
        return argStream;
    }

}
