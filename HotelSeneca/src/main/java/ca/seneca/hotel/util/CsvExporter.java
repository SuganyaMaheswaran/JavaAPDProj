package ca.seneca.hotel.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/** Writes a header row plus data rows to a CSV file, escaping commas/quotes/newlines. */
public final class CsvExporter {

    private CsvExporter() {}

    public static void export(List<String> headers, List<List<String>> rows, File target) throws IOException {
        try (Writer writer = new FileWriter(target)) {
            writer.write(toCsvLine(headers));
            for (List<String> row : rows) {
                writer.write(toCsvLine(row));
            }
        }
    }

    private static String toCsvLine(List<String> values) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                line.append(',');
            }
            line.append(escape(values.get(i)));
        }
        line.append(System.lineSeparator());
        return line.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n");
        String escaped = value.replace("\"", "\"\"");
        return needsQuoting ? "\"" + escaped + "\"" : escaped;
    }
}
