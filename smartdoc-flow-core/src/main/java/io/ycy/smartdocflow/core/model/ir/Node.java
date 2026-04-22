package io.ycy.smartdocflow.core.model.ir;

import io.ycy.smartdocflow.common.model.Bbox;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record Node(
    String id,
    NodeType nodeType,
    String containerId,
    Bbox bbox,
    String text,
    List<TextSpan> spans,
    double confidence,
    String orderKey,
    List<SourceRef> sourceRefs,
    Map<String, Object> properties,
    Set<String> stageTags
) {
}
