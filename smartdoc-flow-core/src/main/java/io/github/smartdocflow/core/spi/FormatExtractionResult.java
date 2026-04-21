package io.github.smartdocflow.core.spi;

import java.util.List;
import java.nio.file.Path;

public record FormatExtractionResult(
    Path source,
    String extractedText,
    List<String> pageTexts,
    List<List<String>> pageLines,
    int pageCount
) {
}
