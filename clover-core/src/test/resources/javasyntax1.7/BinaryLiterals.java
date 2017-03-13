/**
 * In Java SE 7, the integral types (byte, short, int, and long) can also be expressed using the binary number system.
 * To specify a binary literal, add the prefix 0b or 0B to the number.
 *
 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/language/binary-literals.html
 */
public class BinaryLiterals {
    // An 8-bit 'byte' value:
    byte aByte = (byte)0b00100001;

    // A 16-bit 'short' value:
    short aShort = (short)0b1010000101000101;

    // Some 32-bit 'int' values:
    int anInt1 = 0b10100001010001011010000101000101;
    int anInt2 = 0b101;
    int anInt3 = 0B101; // The B can be upper or lower case.

    // Crosscheck if decimal, octal, hex values are instrumented too
    int anInt4 = 123;
    int anInt5 = 0123;
    int anInt6 = 0x123;

    // A 64-bit 'long' value. Note the "L" suffix:
    long aLong = 0b1010000101000101101000010100010110100001010001011010000101000101L;

    /**
     * Simple test - use binary literals in if / switch statements
     * @param instruction
     * @return
     */
    public int switchWithBinary(byte instruction) {
        if ((instruction & 0b11100000) == 0b00000000) {
            return 2;
        } else {
            switch (instruction & 0b11110000) {
                case 0b00000000: return 0;
                case 0b00010000: return 1;
                default: throw new IllegalArgumentException();
            }
        }
    }
}