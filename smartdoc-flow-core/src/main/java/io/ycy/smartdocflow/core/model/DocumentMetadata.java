package io.ycy.smartdocflow.core.model;

import io.ycy.smartdocflow.common.model.DocumentSourceType;

public record DocumentMetadata(
    String documentId,
    String fileName,
    DocumentSourceType sourceType,
    int pageCount,
    String language
) {
}
