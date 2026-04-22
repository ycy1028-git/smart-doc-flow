package io.ycy.smartdocflow.core.spi;

import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;

public interface OcrProvider {
    void process(DocumentIr ir, DocumentProfile profile);
}
