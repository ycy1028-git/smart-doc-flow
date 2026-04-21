package io.github.smartdocflow.core.model;

import io.github.smartdocflow.common.model.Bbox;

public record DocumentBlock(
    String id,
    BlockType type,
    int page,
    Bbox bbox,
    int order,
    String text,
    double confidence
) {
}
