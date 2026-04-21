package io.github.smartdocflow.core.spi;

import io.github.smartdocflow.core.model.DocumentProfile;
import java.nio.file.Path;

public interface FormatExtractor {
    FormatExtractionResult extract(Path source, DocumentProfile profile);
}
