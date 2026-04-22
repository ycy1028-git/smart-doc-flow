package io.ycy.smartdocflow.core.spi;

import io.ycy.smartdocflow.core.model.ir.DocumentIr;

public interface TableRecoverer {
    void recover(DocumentIr ir);
}
