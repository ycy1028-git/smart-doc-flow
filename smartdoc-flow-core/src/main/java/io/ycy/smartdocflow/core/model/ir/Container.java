package io.ycy.smartdocflow.core.model.ir;

import io.ycy.smartdocflow.common.model.Bbox;
import java.util.Map;

public record Container(
    String id,
    ContainerType type,
    int index,
    String label,
    double width,
    double height,
    Bbox bbox,
    Map<String, Object> properties
) {
}
