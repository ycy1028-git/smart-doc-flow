package io.ycy.smartdocflow.core.model.ir;

public record SourceRef(
    String sourceType,
    String refId,
    int offset,
    int length
) {
}
