package com.atlassian.clover.util.format;

import clover.com.lowagie.text.Chunk;
import clover.com.lowagie.text.Font;
import clover.com.lowagie.text.FontFactory;
import clover.com.lowagie.text.Phrase;

import java.awt.Color;

public class PDFFormatter {

    public static Phrase format(String msg, String font, int points, Color anchorColour) {

        boolean bold = false;
        boolean italic = false;
        try {
            Phrase message = new Phrase(10, "", getFont(font, points, bold, italic));
            MessageTokenizer tokenizer = new MessageTokenizer(msg);
            while (tokenizer.hasNext()) {
                int token = tokenizer.nextToken();
                switch (token) {
                    case MessageTokenizer.BOLD_START:
                        bold = true;
                        break;
                    case MessageTokenizer.BOLD_END:
                        bold = false;
                        break;
                    case MessageTokenizer.ITALIC_START:
                        italic = true;
                        break;
                    case MessageTokenizer.ITALIC_END:
                        italic = false;
                        break;
                    case MessageTokenizer.TEXT:
                        message.add(new Phrase(tokenizer.getContent(), getFont(font, points, bold, italic)));
                        break;
                    case MessageTokenizer.ANCHOR_START:
                        StringBuilder buffer = new StringBuilder();
                        token = tokenizer.nextToken();
                        while (token != MessageTokenizer.ANCHOR_END) {
                            buffer.append(tokenizer.getContent());
                            token = tokenizer.nextToken();
                        }
                        String anchor = buffer.toString();
                        Chunk link = new Chunk(anchor,
                                FontFactory.getFont(font, points, Font.BOLD | Font.UNDERLINE,
                                        anchorColour));
                        link.setAnchor(anchor);
                        message.add(link);
                    default:
                        break;
                }
            }
            return message;
        } catch (MessageFormatException e) {
            return new Phrase(10, msg, FontFactory.getFont(font, points, Font.NORMAL));
        }
    }

    private static Font getFont(String font, int points, boolean bold, boolean italic) {
        if (bold && italic) {
            return FontFactory.getFont(font, points, Font.BOLDITALIC);
        } else if (bold) {
            return FontFactory.getFont(font, points, Font.BOLD);
        } else if (italic) {
            return FontFactory.getFont(font, points, Font.ITALIC);
        } else {
            return FontFactory.getFont(font, points, Font.NORMAL);
        }
    }
}
