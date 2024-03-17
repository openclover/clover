package org.openclover.core.reporters.pdf;

import clover.com.lowagie.text.Chunk;
import clover.com.lowagie.text.Document;
import clover.com.lowagie.text.DocumentException;
import clover.com.lowagie.text.Font;
import clover.com.lowagie.text.FontFactory;
import clover.com.lowagie.text.Image;
import clover.com.lowagie.text.Phrase;
import clover.com.lowagie.text.Rectangle;
import clover.com.lowagie.text.pdf.BaseFont;
import clover.com.lowagie.text.pdf.PdfContentByte;
import clover.com.lowagie.text.pdf.PdfPTable;
import clover.com.lowagie.text.pdf.PdfPageEventHelper;
import clover.com.lowagie.text.pdf.PdfTemplate;
import clover.com.lowagie.text.pdf.PdfWriter;
import org.openclover.core.util.format.PDFFormatter;
import org.openclover.runtime.util.Formatting;
import org_openclover_runtime.CloverVersionInfo;

import java.io.IOException;
import java.util.Date;

public class PageFooterRenderer
        extends PdfPageEventHelper {

    // This is the contentbyte object of the writer
    private PdfContentByte cb;

    // we will put the final number of pages in a template
    private PdfTemplate footerTmpl;

    // we will put the final number of pages in a template
    private PdfTemplate totalPageTmpl;

    private BaseFont pageNumFont = null;
    private BaseFont licenseFont = null;

    private float footerWidth;

    private static final int SCALED_LOGO_SIZE = 32;
    private static final int FOOTER_FONT_SIZE = 8;

    private final String timestamp;
    private final Rectangle pgsize;
    private final PDFColours colours;

    public PageFooterRenderer(Rectangle size, long generatedTS, PDFColours colours) {
        pgsize = size;
        timestamp = Formatting.formatDate(new Date(generatedTS));
        this.colours = colours;
    }

    // we override the onOpenDocument method
    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        try {
            pageNumFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            licenseFont = BaseFont.createFont(BaseFont.HELVETICA_OBLIQUE, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            cb = writer.getDirectContent();
            footerWidth = pgsize.width() - 25 - 25; //## HACK magic - 2*page margin
            footerTmpl = cb.createTemplate(footerWidth, SCALED_LOGO_SIZE);
            Image logo = Image.getInstance(getClass().getClassLoader().getResource("pdf_res/logo1.png"));
            footerTmpl.addImage(logo, SCALED_LOGO_SIZE, 0, 0, SCALED_LOGO_SIZE, 0, 0);
            footerTmpl.setLineWidth(0.5f);
            footerTmpl.setColorStroke(colours.COL_TABLE_BORDER);
            footerTmpl.moveTo(0, SCALED_LOGO_SIZE);
            footerTmpl.lineTo(footerWidth, SCALED_LOGO_SIZE);
            footerTmpl.stroke();

            PdfPTable footerTab = new PdfPTable(2);
            footerTab.setTotalWidth(footerWidth - SCALED_LOGO_SIZE - 60); //##HACK - fudge factor
            footerTab.setWidths(new int[]{30, 70});
            footerTab.getDefaultCell().setBorder(Rectangle.RIGHT);
            footerTab.getDefaultCell().setBorderWidth(0.5f);

            footerTab.getDefaultCell().setBorderColor(colours.COL_TABLE_BORDER);
            footerTab.getDefaultCell().setPadding(2f);
            footerTab.getDefaultCell().setLeading(2f, 0.9f);

            Phrase licText = new Phrase("Report generated by ", FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC));

            licText.add(new Chunk("OpenClover v" + CloverVersionInfo.RELEASE_NUM,
                    FontFactory.getFont(
                            FontFactory.HELVETICA, 8, Font.ITALIC,
                            colours.COL_LINK_TEXT)).setAnchor(CloverVersionInfo.CLOVER_URL));
            licText.add(new Phrase("\n" + timestamp,
                    FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC)));

            footerTab.addCell(licText);

            footerTab.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            footerTab.getDefaultCell().setPaddingRight(2f);
            footerTab.getDefaultCell().setPaddingLeft(6f);

            footerTab.writeSelectedRows(0, -1, SCALED_LOGO_SIZE + 2, SCALED_LOGO_SIZE - 2, footerTmpl);
            totalPageTmpl = cb.createTemplate(25, 25);

        } catch (DocumentException de) {
            throw new RuntimeException(de);
            //##HACK - what to do here?
            //de.printStackTrace();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
            //##HACK - what to do here?
            //ioe.printStackTrace();
        }
    }


    // we override the onEndPage method
    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        int pageN = writer.getPageNumber();
        String text = "Page " + pageN + " of ";
        float len = pageNumFont.getWidthPoint(text, FOOTER_FONT_SIZE);
        float max = pageNumFont.getWidthPoint("99", FOOTER_FONT_SIZE);
        int FROM_BOTTOM = 0;// ##HACK - test how close to the bottom to go

        cb.beginText();
        cb.setFontAndSize(pageNumFont, FOOTER_FONT_SIZE);
        cb.setTextMatrix(25 + footerWidth - len - max, FROM_BOTTOM + SCALED_LOGO_SIZE - FOOTER_FONT_SIZE - 2);
        cb.showText(text);
        cb.endText();
        cb.addTemplate(footerTmpl, 25, FROM_BOTTOM);
        cb.addTemplate(totalPageTmpl, 25 + footerWidth - max, FROM_BOTTOM + SCALED_LOGO_SIZE - FOOTER_FONT_SIZE - 2);
    }

    // we override the onCloseDocument method
    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        totalPageTmpl.beginText();
        totalPageTmpl.setFontAndSize(pageNumFont, FOOTER_FONT_SIZE);
        totalPageTmpl.showText(String.valueOf(writer.getPageNumber() - 1));
        totalPageTmpl.endText();
    }

}
