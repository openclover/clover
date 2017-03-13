/**
 * This class tests correct usage of underscores in numeric literals and should compile and instrument correctly.
 *
 * In Java SE 7 and later, any number of underscore characters (_) can appear anywhere between digits in a numerical literal.
 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/language/underscores-literals.html
 */
public class UnderscoresInNumericLiterals {
    long creditCardNumber = 1234_5678_9012_3456L;
    long socialSecurityNumber = 999_99_9999L;
    float pi = 	3.14_15F;
    long hexBytes = 0xFF_EC_DE_5E;
    long hexWords = 0xCAFE_BABE;
    long maxLong = 0x7fff_ffff_ffff_ffffL;
    byte nybbles = 0b0010_0101;
    long bytes = 0b11010010_01101001_10010100_10010010;
}