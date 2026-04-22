package io.ycy.smartdocflow.layout;

import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.model.ir.Node;
import io.ycy.smartdocflow.core.model.ir.NodeType;
import io.ycy.smartdocflow.core.spi.BlockClassifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public final class BasicClassifier implements BlockClassifier {
    @Override
    public void classify(DocumentIr ir) {
        var oldNodes = new ArrayList<>(ir.getNodes());
        ir.getNodes().clear();

        for (Node node : oldNodes) {
            NodeType classifiedType = node.nodeType();
            if (node.nodeType() == NodeType.PARAGRAPH) {
                classifiedType = classifyText(node.text());
            }
            ir.addNode(new Node(
                node.id(),
                classifiedType,
                node.containerId(),
                node.bbox(),
                node.text(),
                node.spans(),
                node.confidence(),
                node.orderKey(),
                node.sourceRefs(),
                node.properties(),
                mergeTag(node.stageTags(), "CLASSIFY")
            ));
        }
    }

    private NodeType classifyText(String text) {
        if (isHeadingCandidate(text)) {
            return NodeType.HEADING;
        }
        if (isTableCandidate(text)) {
            return NodeType.TABLE;
        }
        return NodeType.PARAGRAPH;
    }

    private boolean isHeadingCandidate(String text) {
        String singleLine = text.replace('\n', ' ').trim();
        if (singleLine.isBlank()) {
            return false;
        }
        if (singleLine.length() > 60) {
            return false;
        }
        if (singleLine.endsWith("。") || singleLine.endsWith(".") || singleLine.endsWith(";") || singleLine.endsWith("；") || singleLine.endsWith(":")) {
            return false;
        }
        long lineCount = Arrays.stream(text.split("\\n")).filter(line -> !line.isBlank()).count();
        if (lineCount > 2) {
            return false;
        }
        if (singleLine.contains("    ")) {
            return false;
        }
        return true;
    }

    private boolean isTableCandidate(String text) {
        String normalized = text.strip();
        if (normalized.isBlank()) {
            return false;
        }
        if (normalized.length() > 200) {
            return false;
        }
        String[] columns = normalized.split("\\s{2,}|\\t+|\\s\\|\\s|\\|");
        long nonBlankColumns = Arrays.stream(columns).map(String::strip).filter(value -> !value.isBlank()).count();
        if (nonBlankColumns < 3) {
            return false;
        }
        boolean hasNumericLikeCell = Arrays.stream(columns)
            .map(String::strip)
            .anyMatch(this::isNumericLike);
        return hasNumericLikeCell || normalized.contains("|") || normalized.contains("\t");
    }

    private boolean isNumericLike(String value) {
        return value.matches("[-+]?\\d[\\d,.:()%/-]*");
    }

    private Set<String> mergeTag(Set<String> tags, String tag) {
        var merged = new java.util.LinkedHashSet<>(tags);
        merged.add(tag);
        return merged;
    }
}
