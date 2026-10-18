package com.belegflow.domain;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import static com.belegflow.domain.BusinessException.require;
public final class FilePolicy {
    public static final int MAX_BYTES = 10 * 1024 * 1024;
    private FilePolicy() {
    }
    public static String validate(String filename, byte[] bytes, boolean original) {
        require(filename != null && filename.length() <= 180 && bytes != null && bytes.length > 0 && bytes.length <= MAX_BYTES, "INVALID_FILE");
        require(!filename.contains("/") && !filename.contains("\\") && !filename.contains("\0") && filename.chars().noneMatch(Character::isISOControl), "INVALID_FILE");
        String name = filename.toLowerCase(Locale.ROOT);
        String head = new String(bytes, 0, Math.min(bytes.length, 256), StandardCharsets.UTF_8).strip();
        if (name.endsWith(".pdf") && head.startsWith("%PDF-")) return "application/pdf";
        if (original && name.endsWith(".xml") && (head.startsWith("<") || head.startsWith("\uFEFF<"))) return "application/xml";
        if (!original && name.endsWith(".png") && bytes.length >= 8 && (bytes[0] & 255) == 137 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71 && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10) return "image/png";
        if (!original && (name.endsWith(".jpg") || name.endsWith(".jpeg")) && bytes.length > 3 && (bytes[0]&255)==255 && (bytes[1]&255)==216 && (bytes[2]&255)==255) return "image/jpeg";
        if (!original && name.endsWith(".txt") && bytes.length <= 128 * 1024 && !head.contains("\0")) return "text/plain";
        throw new BusinessException("UNSUPPORTED_FILE");
    }
}
