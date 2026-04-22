package io.ycy.smartdocflow.core.model.ir;

public record Diagnostic(
    String stage,
    String key,
    Object value,
    long timestamp
) {
}
