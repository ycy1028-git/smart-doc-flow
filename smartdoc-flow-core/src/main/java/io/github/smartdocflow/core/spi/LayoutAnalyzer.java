package io.github.smartdocflow.core.spi;

import io.github.smartdocflow.core.model.DocumentProfile;

public interface LayoutAnalyzer {
    LayoutResult analyze(FormatExtractionResult extractionResult, OcrResult ocrResult, DocumentProfile profile);
}
