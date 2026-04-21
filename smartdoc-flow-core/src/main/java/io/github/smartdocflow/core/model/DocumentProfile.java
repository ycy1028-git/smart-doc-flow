package io.github.smartdocflow.core.model;

import io.github.smartdocflow.common.model.DocumentSourceType;

public record DocumentProfile(
    DocumentSourceType sourceType,
    boolean scanned,
    boolean multiColumn,
    boolean tableHeavy,
    boolean imageHeavy
) {
}
