package com.atlassian.clover.util.format;

public class HtmlFormatter {

    public static String format(String message) {
        try {
            StringBuilder buffer = new StringBuilder();
            MessageTokenizer tokenizer = new MessageTokenizer(message);
            while (tokenizer.hasNext()) {
                int token = tokenizer.nextToken();
                if (token == MessageTokenizer.ANCHOR_START) {
                    buffer.append(readAnchor(tokenizer));
                } else if (token == MessageTokenizer.HORIZONTAL_LINE) {
                    buffer.append("<hr>");
                } else {
                    buffer.append(tokenizer.getContent());
                }
            }
            String formatted = buffer.toString();
            // replace all of the new line characters.
            return StringFormatting.replaceAll(formatted, "\n", "<br/>");
        } catch (MessageFormatException e) {
            return message;
        }
    }



    private static String readAnchor(MessageTokenizer tokenizer) throws MessageFormatException {
        StringBuilder buffer = new StringBuilder();
        int token = 0;
        while (tokenizer.hasNext()) {
            token = tokenizer.nextToken();
            if (token == MessageTokenizer.ANCHOR_END) {
                break;
            }
            buffer.append(tokenizer.getContent());
        }
        String anchor = buffer.toString();
        return "<a href=\"" + anchor + "\">" + anchor + "</a>";
    }

}
