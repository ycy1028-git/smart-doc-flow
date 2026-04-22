package io.ycy.smartdocflow.core.model;

import io.ycy.smartdocflow.common.model.Bbox;

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
