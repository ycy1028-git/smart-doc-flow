package io.ycy.smartdocflow.layout;

import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.spi.PostProcessor;

public final class NoopPostProcessor implements PostProcessor {
    @Override
    public void process(DocumentIr ir) {
    }
}
