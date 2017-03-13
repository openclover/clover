package simple;

public class UnicodeChars {
    // unicode char at start of ident
    int Ätest;

    // unicode char in middle of ident
    int testÄtest;

    // escaped unicode ident ("sanity")
    int \u0073\u0061\u006e\u0069\u0074\u0079;

    // escaped unicode in comments
    /** \u0000 */

    // \u0000

    // a backslash as escaped unicode escaping a single quote
    char SINGLE_QUOTE = '\u005c'';

    // a backslash as escaped unicode escaping a double quote
    String DOUBLE_QUOTE = "\u005c"";

    // a backslash as escaped unicode esaping another escaped unicode
    /** \u005Cu0020 */
    /** {@code \u005Cu}<i>xxxx</i> */

}
