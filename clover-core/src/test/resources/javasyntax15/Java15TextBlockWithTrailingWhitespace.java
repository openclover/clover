public class Java15TextBlockWithTrailingWhitespace {

    // the opening """ delimiter may be followed by whitespace
    public String singleBlockWithTrailingWhitespaceAfterOpening() {
        return """ 
                hello
                """;
    }

    // two empty text blocks concatenated with a variable in between,
    // where the second block's opening delimiter has trailing whitespace
    public String concatenatedEmptyBlocksWithTrailingWhitespace(String marker) {
        return """
        """ 
        + marker + 
        """ 
        """;
    }
}
