import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class PersonTest {
    @Parameterized.Parameters(name = "{0} is a {1} [{index}]")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Alice", "woman"}, {"Bob", "man"}, {"Rex", "unknown"}
        });
    }

    protected String input;
    protected String expected;

    public PersonTest(String input, String expected) {
        this.input = input;
        this.expected = expected;
    }

    @Test
    public void test() {
        assertEquals(expected, new Person(input).getSex());
    }
}
