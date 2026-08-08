package ca.seneca.hotel.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/** Writes a header row plus data rows to a plain-text file as fixed-width, space-padded columns. */
public final class TxtExporter {

    private static final int COLUMN_PADDING = 2;

    private TxtExporter() {}

    public static void export(List<String> headers, List<List<String>> rows, File target) throws IOException {
        int[] widths = columnWidths(headers, rows);

        try (Writer writer = new FileWriter(target)) {
            writer.write(toLine(headers, widths));
            writer.write("-".repeat(java.util.Arrays.stream(widths).sum() + widths.length * COLUMN_PADDING));
            writer.write(System.lineSeparator());
            for (List<String> row : rows) {
                writer.write(toLine(row, widths));
            }
        }
    }

    private static int[] columnWidths(List<String> headers, List<List<String>> rows) {
        int[] widths = new int[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            widths[i] = headers.get(i).length();
        }
        for (List<String> row : rows) {
            for (int i = 0; i < row.size() && i < widths.length; i++) {
                String cell = row.get(i) == null ? "" : row.get(i);
                widths[i] = Math.max(widths[i], cell.length());
            }
        }
        return widths;
    }

    private static String toLine(List<String> values, int[] widths) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < widths.length; i++) {
            String cell = i < values.size() && values.get(i) != null ? values.get(i) : "";
            line.append(cell);
            line.append(" ".repeat(widths[i] - cell.length() + COLUMN_PADDING));
        }
        line.append(System.lineSeparator());
        return line.toString();
    }
}
