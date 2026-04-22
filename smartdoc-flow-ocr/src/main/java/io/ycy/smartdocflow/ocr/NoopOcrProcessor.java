package io.ycy.smartdocflow.ocr;

import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.spi.FormatExtractionResult;
import io.ycy.smartdocflow.core.spi.OcrProcessor;
import io.ycy.smartdocflow.core.spi.OcrResult;

public final class NoopOcrProcessor implements OcrProcessor {
    @Override
    public OcrResult process(FormatExtractionResult extractionResult, DocumentProfile profile) {
        if (profile.scanned()) {
            return new OcrResult("OCR placeholder for " + extractionResult.source().getFileName(), true);
        }
        return OcrResult.skipped();
    }
}
