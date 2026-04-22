package io.ycy.smartdocflow.core.model.ir;

import java.util.Map;

public record Relation(
    String fromId,
    String toId,
    RelationType relationType,
    double confidence,
    Map<String, Object> properties
) {
}
