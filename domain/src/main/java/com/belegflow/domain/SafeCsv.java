package com.belegflow.domain;
import java.nio.charset.StandardCharsets;
import java.util.List;
/** RFC4180-style quoting + formula neutralization. CSV is a handoff, not a DATEV format. */
public final class SafeCsv {
    private SafeCsv() {
    }
    public static String cell(Object value) {
        String text = value == null ? "" : value.toString();
        String trimmed = text.stripLeading();
        if (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
    public static byte[] encode(List<List<Object>> rows) {
        StringBuilder output = new StringBuilder("\uFEFF");
        for (List<Object> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) output.append(';');
                output.append(cell(row.get(i)));
            }
            output.append("\r\n");
        }
        return output.toString().getBytes(StandardCharsets.UTF_8);
    }
}
