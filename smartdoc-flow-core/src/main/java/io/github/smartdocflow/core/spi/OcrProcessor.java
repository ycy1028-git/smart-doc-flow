package io.github.smartdocflow.core.spi;

import io.github.smartdocflow.core.model.DocumentProfile;

public interface OcrProcessor {
    OcrResult process(FormatExtractionResult extractionResult, DocumentProfile profile);
}
