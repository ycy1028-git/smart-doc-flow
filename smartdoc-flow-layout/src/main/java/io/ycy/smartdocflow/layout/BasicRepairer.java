package io.ycy.smartdocflow.layout;

import io.ycy.smartdocflow.common.model.Bbox;
import io.ycy.smartdocflow.core.model.ir.Container;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.model.ir.Node;
import io.ycy.smartdocflow.core.model.ir.NodeType;
import io.ycy.smartdocflow.core.model.ir.SourceRef;
import io.ycy.smartdocflow.core.spi.StructureRepairer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BasicRepairer implements StructureRepairer {
    @Override
    public void repair(DocumentIr ir) {
        if (ir.getNodes().isEmpty() && ir.getContainers().isEmpty()) {
            return;
        }

        List<Node> repaired = new ArrayList<>();
        List<Node> currentParagraph = new ArrayList<>();
        String currentContainerId = null;
        int order = 0;

        for (Node node : ir.getNodes()) {
            if (node.nodeType() == NodeType.HEADER || node.nodeType() == NodeType.FOOTER || node.nodeType() == NodeType.PAGE_NUMBER) {
                continue;
            }

            if (node.containerId() != null && !node.containerId().equals(currentContainerId)) {
                if (!currentParagraph.isEmpty()) {
                    repaired.add(joinParagraph(currentParagraph, order++, currentContainerId));
                    currentParagraph.clear();
                }
                currentContainerId = node.containerId();
            }

            if (node.nodeType() == NodeType.HEADING || node.nodeType() == NodeType.TITLE) {
                if (!currentParagraph.isEmpty()) {
                    repaired.add(joinParagraph(currentParagraph, order++, currentContainerId));
                    currentParagraph.clear();
                }
                repaired.add(withTag(node, "REPAIR", order++));
                continue;
            }

            if (node.nodeType() == NodeType.TABLE || node.nodeType() == NodeType.FIGURE || node.nodeType() == NodeType.FORMULA || node.nodeType() == NodeType.CODE_BLOCK) {
                if (!currentParagraph.isEmpty()) {
                    repaired.add(joinParagraph(currentParagraph, order++, currentContainerId));
                    currentParagraph.clear();
                }
                repaired.add(withTag(node, "REPAIR", order++));
                continue;
            }

            currentParagraph.add(node);
        }

        if (!currentParagraph.isEmpty()) {
            repaired.add(joinParagraph(currentParagraph, order, currentContainerId));
        }

        ir.getNodes().clear();
        repaired.forEach(ir::addNode);
    }

    private Node joinParagraph(List<Node> parts, int order, String containerId) {
        if (parts.size() == 1) {
            return withTag(parts.getFirst(), "REPAIR", order);
        }

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                text.append(joinerBetween(parts.get(i - 1).text(), parts.get(i).text()));
            }
            text.append(parts.get(i).text().strip());
        }

        return new Node(
            UUID.randomUUID().toString(),
            NodeType.PARAGRAPH,
            containerId,
            Bbox.EMPTY,
            text.toString().trim(),
            List.of(),
            parts.stream().mapToDouble(Node::confidence).average().orElse(0.95),
            String.valueOf(order),
            List.of(),
            Map.of(),
            Set.of("REPAIR")
        );
    }

    private Node withTag(Node node, String tag, int order) {
        var tags = new java.util.LinkedHashSet<>(node.stageTags());
        tags.add(tag);
        return new Node(
            node.id(),
            node.nodeType(),
            node.containerId(),
            node.bbox(),
            node.text(),
            node.spans(),
            node.confidence(),
            String.valueOf(order),
            node.sourceRefs(),
            node.properties(),
            tags
        );
    }

    private String joinerBetween(String previousText, String nextLine) {
        String prev = previousText.stripTrailing();
        String next = nextLine.stripLeading();
        if (prev.isEmpty() || next.isEmpty()) {
            return " ";
        }
        char prevLast = prev.charAt(prev.length() - 1);
        char nextFirst = next.charAt(0);
        if (prevLast == '-') {
            return "";
        }
        if (isSentenceEnding(prevLast)) {
            return "\n";
        }
        if (Character.isLowerCase(nextFirst) || Character.isDigit(nextFirst)) {
            return " ";
        }
        if (isCjk(nextFirst)) {
            return "";
        }
        return " ";
    }

    private boolean isSentenceEnding(char value) {
        return value == '.' || value == '!' || value == '?' || value == '。' || value == '！' || value == '？' || value == ':' || value == '：' || value == ';' || value == '；';
    }

    private boolean isCjk(char value) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(value);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
            || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }
}
