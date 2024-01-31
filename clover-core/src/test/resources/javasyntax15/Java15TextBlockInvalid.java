public class Java15TextBlockInvalid {
    public void invalidTextBlocks() {
        // no line terminator
        String a = """""";

        // no line terminator
        String b = """ """;

        // no closing delimiter (text block continues to EOF)
        //String c = """
        //   ";

        // unescaped backslash (see below for escape processing)
        String d = """
                abc \ def
                """;
    }
}