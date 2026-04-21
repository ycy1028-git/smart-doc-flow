package io.github.smartdocflow.core.model;

import io.github.smartdocflow.common.model.DocumentSourceType;

public record DocumentMetadata(
    String documentId,
    String fileName,
    DocumentSourceType sourceType,
    int pageCount,
    String language
) {
}
