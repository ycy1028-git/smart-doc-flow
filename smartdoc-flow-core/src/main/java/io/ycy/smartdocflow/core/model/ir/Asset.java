package io.ycy.smartdocflow.core.model.ir;

import io.ycy.smartdocflow.common.model.Bbox;
import java.util.Map;

public record Asset(
    String id,
    String assetType,
    String containerId,
    String name,
    String mimeType,
    long size,
    String ref,
    Bbox bbox,
    Map<String, Object> properties
) {
}
