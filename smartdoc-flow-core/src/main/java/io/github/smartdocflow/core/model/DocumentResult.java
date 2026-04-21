package io.github.smartdocflow.core.model;

import java.util.List;

public record DocumentResult(
    DocumentMetadata metadata,
    List<DocumentBlock> blocks,
    List<DocumentAsset> assets
) {
}
