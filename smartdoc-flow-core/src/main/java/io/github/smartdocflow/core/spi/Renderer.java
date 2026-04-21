package io.github.smartdocflow.core.spi;

import io.github.smartdocflow.core.model.DocumentResult;

public interface Renderer {
    String render(DocumentResult documentResult);
}
