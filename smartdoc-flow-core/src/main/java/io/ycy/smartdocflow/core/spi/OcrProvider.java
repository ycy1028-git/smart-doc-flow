package io.ycy.smartdocflow.core.spi;

import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import java.nio.file.Path;

public interface OcrProvider {
    void process(Path source, DocumentIr ir, DocumentProfile profile);
}
