/**
 *
 */
public class Person {
    private String name;

    public Person(String name) {
        this.name = name;
    }

    public String getSex() {
        return "Alice".equals(name) ? "woman" : "man";
    }
}
