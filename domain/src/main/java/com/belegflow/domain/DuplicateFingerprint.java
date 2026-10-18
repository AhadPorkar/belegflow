package com.belegflow.domain;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
public final class DuplicateFingerprint {
    private DuplicateFingerprint() {
    }
    public static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
    public static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC) .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }
    /** Supplier identity + normalized invoice number: warns even when the amount changed. */
    public static String businessKey(UUID supplierId, String invoiceNumber) {
        return sha256((supplierId + ":" + normalize(invoiceNumber)).getBytes(StandardCharsets.UTF_8));
    }
}
