package com.atlassian.clover.util.format;

public class ConsoleFormatter {

    public static String format(String message) {
        StringBuilder buffer = new StringBuilder();
        try {
            MessageTokenizer tokenizer = new MessageTokenizer(message);
            while (tokenizer.hasNext()) {
                int nextToken = tokenizer.nextToken();
                switch (nextToken) {
                    case MessageTokenizer.TEXT:
                    case MessageTokenizer.HORIZONTAL_LINE:
                        buffer.append(tokenizer.getContent());
                        break;
                }
            }
            return buffer.toString();
        } catch (MessageFormatException e) {
            return message;
        }
    }
}
