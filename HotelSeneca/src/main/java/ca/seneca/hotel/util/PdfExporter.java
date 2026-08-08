package ca.seneca.hotel.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/** Renders a title plus a simple paginated table (equal-width columns) to a PDF file via PDFBox. */
public final class PdfExporter {

    private static final float MARGIN = 40;
    private static final float ROW_HEIGHT = 18;
    private static final float FONT_SIZE = 9;
    private static final float TITLE_FONT_SIZE = 16;

    private PdfExporter() {}

    public static void export(String title, List<String> headers, List<List<String>> rows, File target) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            PDPageContentStream content = new PDPageContentStream(document, page);

            float pageWidth = page.getMediaBox().getWidth() - 2 * MARGIN;
            float pageHeight = page.getMediaBox().getHeight();
            float[] columnWidths = columnWidths(headers, pageWidth);

            float y = writeTitle(content, title, pageHeight - MARGIN);
            y = writeRow(content, headers, columnWidths, y, true);

            for (List<String> row : rows) {
                if (y < MARGIN + ROW_HEIGHT) {
                    content.close();
                    page = new PDPage(PDRectangle.LETTER);
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    y = pageHeight - MARGIN;
                    y = writeRow(content, headers, columnWidths, y, true);
                }
                y = writeRow(content, row, columnWidths, y, false);
            }
            content.close();
            document.save(target);
        }
    }

    private static float writeTitle(PDPageContentStream content, String title, float y) throws IOException {
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, TITLE_FONT_SIZE);
        content.newLineAtOffset(MARGIN, y);
        content.showText(title);
        content.endText();
        return y - TITLE_FONT_SIZE - 14;
    }

    private static float writeRow(PDPageContentStream content, List<String> values, float[] columnWidths,
                                  float y, boolean bold) throws IOException {
        content.setFont(bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, FONT_SIZE);
        float x = MARGIN;
        for (int i = 0; i < values.size() && i < columnWidths.length; i++) {
            String text = truncate(values.get(i) == null ? "" : values.get(i), columnWidths[i]);
            content.beginText();
            content.newLineAtOffset(x, y);
            content.showText(text);
            content.endText();
            x += columnWidths[i];
        }
        return y - ROW_HEIGHT;
    }

    private static float[] columnWidths(List<String> headers, float pageWidth) {
        float[] widths = new float[headers.size()];
        Arrays.fill(widths, pageWidth / headers.size());
        return widths;
    }

    /** Rough character-count cap so a long value doesn't overrun into the next column. */
    private static String truncate(String text, float columnWidth) {
        int maxChars = (int) (columnWidth / (FONT_SIZE * 0.55f));
        return text.length() > maxChars ? text.substring(0, Math.max(0, maxChars - 1)) + "..." : text;
    }
}
