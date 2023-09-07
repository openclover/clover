/**
 *
 */
public class Person {

    public static final String MALE = "MALE";
    public static final String FEMALE = "FEMALE";
    public static final String UNKNOWN = "UNKNOWN";

    private String name;

    public Person(String name) {
        this.name = name;
    }

    public String getGender() {
        if (this.name == null) return null;
        String name = this.name.toLowerCase();
        if (name.contains("ms.") || name.contains("mrs.")) {
            return FEMALE;
        } else if (name.contains("mr.")) {
            return MALE;
        } else {
            return UNKNOWN;
        }
    }
}
