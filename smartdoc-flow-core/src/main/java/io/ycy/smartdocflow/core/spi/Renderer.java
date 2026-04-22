package io.ycy.smartdocflow.core.spi;

import io.ycy.smartdocflow.core.model.DocumentResult;

public interface Renderer {
    String render(DocumentResult documentResult);
}
