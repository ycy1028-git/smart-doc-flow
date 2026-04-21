package io.github.smartdocflow.ocr;

import io.github.smartdocflow.core.model.DocumentProfile;
import io.github.smartdocflow.core.spi.FormatExtractionResult;
import io.github.smartdocflow.core.spi.OcrProcessor;
import io.github.smartdocflow.core.spi.OcrResult;

public final class NoopOcrProcessor implements OcrProcessor {
    @Override
    public OcrResult process(FormatExtractionResult extractionResult, DocumentProfile profile) {
        if (profile.scanned()) {
            return new OcrResult("OCR placeholder for " + extractionResult.source().getFileName(), true);
        }
        return OcrResult.skipped();
    }
}
