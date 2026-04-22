package io.ycy.smartdocflow.core.spi;

import io.ycy.smartdocflow.core.model.DocumentProfile;

public interface LayoutAnalyzer {
    LayoutResult analyze(FormatExtractionResult extractionResult, OcrResult ocrResult, DocumentProfile profile);
}
