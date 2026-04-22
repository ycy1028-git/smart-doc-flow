package io.ycy.smartdocflow.core.spi;

import io.ycy.smartdocflow.core.model.ParseOptions;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;

public interface ResultRenderer {
    String render(DocumentIr ir, ParseOptions options);
}
