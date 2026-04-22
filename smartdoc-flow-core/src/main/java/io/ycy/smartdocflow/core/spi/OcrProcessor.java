package io.ycy.smartdocflow.core.spi;

import io.ycy.smartdocflow.core.model.DocumentProfile;

public interface OcrProcessor {
    OcrResult process(FormatExtractionResult extractionResult, DocumentProfile profile);
}
