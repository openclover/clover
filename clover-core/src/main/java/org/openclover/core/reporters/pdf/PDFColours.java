package org.openclover.core.reporters.pdf;

import java.awt.Color;


public class PDFColours {

    public final Color COL_TABLE_BORDER;
    public final Color COL_HEADER_BG;
    public final Color COL_LINK_TEXT;
    public final Color COL_BAR_COVERED;
    public final Color COL_BAR_UNCOVERED;
    public final Color COL_BAR_BORDER;
    public final Color COL_BAR_NA;


    private PDFColours(Color table_border,
                       Color header_bg,
                       Color link_text,
                       Color bar_covered,
                       Color bar_uncovered,
                       Color bar_border,
                       Color bar_na) {
        COL_TABLE_BORDER = table_border;
        COL_HEADER_BG = header_bg;
        COL_LINK_TEXT = link_text;
        COL_BAR_COVERED = bar_covered;
        COL_BAR_UNCOVERED = bar_uncovered;
        COL_BAR_BORDER = bar_border;
        COL_BAR_NA = bar_na;
    }


    public static final PDFColours BW_COLOURS = new PDFColours(
            new Color(0x9c, 0x9c, 0x9c),
            new Color(0xf7, 0xf7, 0xf7),
            new Color(0, 0, 0),
            new Color(0xbc, 0xbc, 0xbc),
            new Color(0xff, 0xff, 0xff),
            new Color(0x5a, 0x5a, 0x5a),
            new Color(0xe6, 0xe6, 0xe6));

    public static final PDFColours COL_COLOURS = new PDFColours(
            new Color(0x9c, 0x9c, 0x9c),
            new Color(0xef, 0xf7, 0xff),
            new Color(0, 0, 0xff),
            new Color(0, 0xdc, 0),
            new Color(0xdc, 0, 0),
            new Color(0x5a, 0x5a, 0x5a),
            new Color(0xe6, 0xe6, 0xe6));


}
