package com.belegflow.domain;
/** Stable, localizable error code; never put sensitive documents or SQL in an error. */
public final class BusinessException extends RuntimeException {
    private final String code;
    public BusinessException(String code) {
        super(code);
        this.code = code;
    }
    public String code() {
        return code;
    }
    public static void require(boolean condition, String code) {
        if (!condition) throw new BusinessException(code);
    }
}
