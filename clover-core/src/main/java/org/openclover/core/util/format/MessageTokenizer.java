package org.openclover.core.util.format;

public class MessageTokenizer {

    public static final int ANCHOR_START = 1;
    public static final int ANCHOR_END = 2;
    public static final int BOLD_START = 3;
    public static final int BOLD_END = 4;
    public static final int ITALIC_START = 5;
    public static final int ITALIC_END = 6;
    public static final int TEXT = 7;
    public static final int END = -1;

    public static final int HORIZONTAL_LINE = 8;

    private char[] message;
    int nextIndex = 0;

    private String currentContent = null;

    public MessageTokenizer(String aMessage) {
        if (aMessage == null) {
            message = new char[0];
        } else {
            message = aMessage.toCharArray();
        }
    }

    public boolean hasNext() {
        return nextIndex < message.length;
    }

    public int nextToken() throws MessageFormatException {
        if (!hasNext()) {
            return END;
        }
        char c = message[nextIndex];
        if (c == '<') {
            return readToken();
        } else if (c == '*') {
            if (message.length > nextIndex) {
                char next = message[nextIndex + 1];
                if (next == '*') {
                    return readHorizontalLine();
                }
            }
        }
        return readText();

    }

    private int readHorizontalLine() {
        StringBuilder buffer = new StringBuilder();
        while (hasNext()) {
            char c = message[nextIndex];
            if (c != '*') {
                break;
            }
            buffer.append(c);
            nextIndex++;
        }
        currentContent = buffer.toString();
        return HORIZONTAL_LINE;
    }

    private int readText() {
        StringBuilder buffer = new StringBuilder();
        while (hasNext()) {
            char c = message[nextIndex];
            if (c == '<') {
                break;
            } else if (c == '*') {
                if (message.length > nextIndex) {
                    char next = message[nextIndex + 1];
                    if (next == '*') {
                        break;
                    }
                }
            }
            buffer.append(c);
            nextIndex++;
        }
        currentContent = buffer.toString();
        return TEXT;
    }

    private int readToken() throws MessageFormatException {
        int token = 0;
        boolean end = false;

        StringBuilder buffer = new StringBuilder();
        char c = message[nextIndex];
        if (c != '<') {
            throw new MessageFormatException("Expected < but found " + c);
        }
        nextIndex++; // consume token.
        buffer.append(c);
        c = message[nextIndex];
        if (c == '/') {
            end = true;
            nextIndex++; // consume token.
            buffer.append(c);
        }
        c = message[nextIndex];
        if (c == 'A' || c == 'a') {
            token = (end) ? ANCHOR_END : ANCHOR_START;
        } else if (c == 'B' || c == 'b') {
            token = (end) ? BOLD_END : BOLD_START;
        } else if (c == 'I' || c == 'i') {
            token = (end) ? ITALIC_END : ITALIC_START;
        } else {
            throw new MessageFormatException("Expected [aAbBiI] but found " + c);
        }
        nextIndex++; // consume token.
        buffer.append(c);

        c = message[nextIndex];
        if (c != '>') {
            throw new MessageFormatException("Expected > but found " + c);
        }
        nextIndex++;
        buffer.append(c);
        currentContent = buffer.toString();
        return token;
    }

    public String getContent() {
        return currentContent;
    }

}
