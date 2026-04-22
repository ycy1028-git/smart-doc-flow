package io.ycy.smartdocflow.core.model;

import io.ycy.smartdocflow.common.model.DocumentSourceType;

public record DocumentProfile(
    DocumentSourceType sourceType,
    boolean scanned,
    boolean multiColumn,
    boolean tableHeavy,
    boolean imageHeavy
) {
}
