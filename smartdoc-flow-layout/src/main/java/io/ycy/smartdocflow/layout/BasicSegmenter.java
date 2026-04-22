package io.ycy.smartdocflow.layout;

import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.model.ir.Diagnostic;
import io.ycy.smartdocflow.core.model.ir.Node;
import io.ycy.smartdocflow.core.model.ir.SourceRef;
import io.ycy.smartdocflow.core.spi.Segmenter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

public final class BasicSegmenter implements Segmenter {
    @Override
    public void segment(DocumentIr ir) {
        var oldNodes = new ArrayList<>(ir.getNodes());
        ir.getNodes().clear();
        int createdSegments = 0;

        for (Node node : oldNodes) {
            List<String> segments = splitSegments(node.text());
            if (segments.size() <= 1 || !supportsSegmentation(node)) {
                ir.addNode(new Node(
                    node.id(),
                    node.nodeType(),
                    node.containerId(),
                    node.bbox(),
                    node.text(),
                    node.spans(),
                    node.confidence(),
                    node.orderKey(),
                    node.sourceRefs(),
                    node.properties(),
                    mergeTag(node.stageTags(), "SEGMENT")
                ));
                continue;
            }

            for (int index = 0; index < segments.size(); index++) {
                String segment = segments.get(index);
                ir.addNode(new Node(
                    UUID.randomUUID().toString(),
                    node.nodeType(),
                    node.containerId(),
                    node.bbox(),
                    segment,
                    List.of(),
                    node.confidence(),
                    node.orderKey() + "." + index,
                    buildSourceRefs(node, segment),
                    node.properties(),
                    mergeTag(node.stageTags(), "SEGMENT")
                ));
                createdSegments++;
            }
        }
        ir.addDiagnostic(new Diagnostic("SEGMENT", "createdSegments", createdSegments, System.currentTimeMillis()));
    }

    private boolean supportsSegmentation(Node node) {
        return switch (node.nodeType()) {
            case PARAGRAPH, QUOTE, COMMENT -> true;
            default -> false;
        };
    }

    private List<String> splitSegments(String text) {
        if (text == null || text.isBlank()) {
            return List.of(text == null ? "" : text);
        }

        String[] rawSegments = text.split("\\n\\s*\\n+");
        var segments = new ArrayList<String>();
        for (String rawSegment : rawSegments) {
            String cleaned = rawSegment.trim();
            if (!cleaned.isBlank()) {
                segments.add(cleaned);
            }
        }
        return segments.isEmpty() ? List.of(text.trim()) : segments;
    }

    private List<SourceRef> buildSourceRefs(Node node, String segment) {
        if (!node.sourceRefs().isEmpty()) {
            SourceRef original = node.sourceRefs().getFirst();
            return List.of(new SourceRef(original.sourceType(), original.refId(), original.offset(), segment.length()));
        }
        return List.of();
    }

    private java.util.Set<String> mergeTag(java.util.Set<String> tags, String tag) {
        var merged = new LinkedHashSet<>(tags);
        merged.add(tag);
        return merged;
    }
}
