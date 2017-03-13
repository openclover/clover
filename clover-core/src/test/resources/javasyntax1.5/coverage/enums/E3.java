package coverage.enums;

public enum E3
{
    RED,
    GREEN() {
        public String toString() {
            return "it aint easy";
        }
    },
    BLUE
}
