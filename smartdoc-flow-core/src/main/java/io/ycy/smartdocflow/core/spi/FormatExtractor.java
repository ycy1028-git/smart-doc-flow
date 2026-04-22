package io.ycy.smartdocflow.core.spi;

import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import java.nio.file.Path;

public interface FormatExtractor {
    FormatExtractionResult extract(Path source, DocumentProfile profile);

    default void extract(Path source, DocumentProfile profile, DocumentIr ir) {
        FormatExtractionResult result = extract(source, profile);
    }
}
