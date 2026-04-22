package io.ycy.smartdocflow.core.spi;

import io.ycy.smartdocflow.core.model.ir.DocumentIr;

public interface ReadingOrderResolver {
    void resolve(DocumentIr ir);
}
