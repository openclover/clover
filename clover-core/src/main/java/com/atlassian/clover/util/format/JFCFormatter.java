package com.atlassian.clover.util.format;

public class JFCFormatter {

    public static String format(String message) {
        try {
            StringBuilder buffer = new StringBuilder();
            MessageTokenizer tokenizer = new MessageTokenizer(message);
            while (tokenizer.hasNext()) {
                int token = tokenizer.nextToken();
                switch (token) {
                    // ignore anchor tags, since the rendered link
                    // can not be followed from a JLabel.
                    case MessageTokenizer.ANCHOR_START:
                    case MessageTokenizer.ANCHOR_END:
                        // ignore the horizontal lines as well.
                    case MessageTokenizer.HORIZONTAL_LINE:
                        break;
                    default:
                        // return everything else.
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
}