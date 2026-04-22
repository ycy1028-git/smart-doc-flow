package io.ycy.smartdocflow.layout;

import io.ycy.smartdocflow.core.model.ir.Container;
import io.ycy.smartdocflow.core.model.ir.Diagnostic;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.model.ir.Node;
import io.ycy.smartdocflow.core.spi.ReadingOrderResolver;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

public final class BasicReadingOrderResolver implements ReadingOrderResolver {
    @Override
    public void resolve(DocumentIr ir) {
        Map<String, Integer> containerOrder = ir.getContainers().stream()
            .collect(Collectors.toMap(Container::id, Container::index, (left, right) -> left));

        var ordered = new ArrayList<>(ir.getNodes());
        ordered.sort(
            Comparator.comparingInt((Node node) -> containerOrder.getOrDefault(node.containerId(), Integer.MAX_VALUE))
                .thenComparing(this::compareOrderKey)
                .thenComparing(Node::id)
        );

        ir.getNodes().clear();
        for (Node node : ordered) {
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
                mergeTag(node.stageTags(), "ORDER")
            ));
        }
        ir.addDiagnostic(new Diagnostic("ORDER", "orderedNodes", ordered.size(), System.currentTimeMillis()));
    }

    private java.util.List<Integer> parseOrderKey(Node node) {
        if (node.orderKey() == null || node.orderKey().isBlank()) {
            return java.util.List.of(Integer.MAX_VALUE);
        }

        String[] parts = node.orderKey().split("\\.");
        var parsed = new ArrayList<Integer>(parts.length);
        for (String part : parts) {
            try {
                parsed.add(Integer.parseInt(part));
            } catch (NumberFormatException ignored) {
                parsed.add(Integer.MAX_VALUE);
            }
        }
        return parsed;
    }

    private int compareOrderKey(Node left, Node right) {
        var leftParts = parseOrderKey(left);
        var rightParts = parseOrderKey(right);
        int length = Math.max(leftParts.size(), rightParts.size());
        for (int index = 0; index < length; index++) {
            int leftValue = index < leftParts.size() ? leftParts.get(index) : Integer.MAX_VALUE;
            int rightValue = index < rightParts.size() ? rightParts.get(index) : Integer.MAX_VALUE;
            int result = Integer.compare(leftValue, rightValue);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    private java.util.Set<String> mergeTag(java.util.Set<String> tags, String tag) {
        var merged = new LinkedHashSet<>(tags);
        merged.add(tag);
        return merged;
    }
}
