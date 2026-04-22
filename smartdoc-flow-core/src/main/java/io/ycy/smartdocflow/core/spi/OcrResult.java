package io.ycy.smartdocflow.core.spi;

public record OcrResult(String text, boolean applied) {
    public static OcrResult skipped() {
        return new OcrResult("", false);
    }
}
