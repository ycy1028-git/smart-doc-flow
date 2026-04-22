package io.ycy.smartdocflow.layout;

import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.model.ir.Diagnostic;
import io.ycy.smartdocflow.core.model.ir.Node;
import io.ycy.smartdocflow.core.spi.PostProcessor;
import java.util.ArrayList;
import java.util.LinkedHashSet;

public final class NoopPostProcessor implements PostProcessor {
    @Override
    public void process(DocumentIr ir) {
        var oldNodes = new ArrayList<>(ir.getNodes());
        ir.getNodes().clear();
        int removedBlankNodes = 0;
        int removedDuplicateNodes = 0;

        Node previous = null;
        for (Node node : oldNodes) {
            String cleanedText = node.text() == null ? "" : node.text().trim();
            if (cleanedText.isBlank()) {
                removedBlankNodes++;
                continue;
            }

            Node cleanedNode = new Node(
                node.id(),
                node.nodeType(),
                node.containerId(),
                node.bbox(),
                cleanedText,
                node.spans(),
                node.confidence(),
                node.orderKey(),
                node.sourceRefs(),
                node.properties(),
                mergeTag(node.stageTags(), "POST")
            );

            if (isAdjacentDuplicate(previous, cleanedNode)) {
                removedDuplicateNodes++;
                continue;
            }

            ir.addNode(cleanedNode);
            previous = cleanedNode;
        }
        ir.addDiagnostic(new Diagnostic("POST", "removedBlankNodes", removedBlankNodes, System.currentTimeMillis()));
        ir.addDiagnostic(new Diagnostic("POST", "removedDuplicateNodes", removedDuplicateNodes, System.currentTimeMillis()));
    }

    private boolean isAdjacentDuplicate(Node previous, Node current) {
        if (previous == null) {
            return false;
        }
        return java.util.Objects.equals(previous.containerId(), current.containerId())
            && previous.nodeType() == current.nodeType()
            && previous.text().equals(current.text());
    }

    private java.util.Set<String> mergeTag(java.util.Set<String> tags, String tag) {
        var merged = new LinkedHashSet<>(tags);
        merged.add(tag);
        return merged;
    }
}
