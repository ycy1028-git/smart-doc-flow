package io.ycy.smartdocflow.layout;

import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.model.ir.Diagnostic;
import io.ycy.smartdocflow.core.model.ir.Node;
import io.ycy.smartdocflow.core.model.ir.NodeType;
import io.ycy.smartdocflow.core.model.ir.SourceRef;
import io.ycy.smartdocflow.core.spi.TableRecoverer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BasicTableRecoverer implements TableRecoverer {
    @Override
    public void recover(DocumentIr ir) {
        var oldNodes = new ArrayList<>(ir.getNodes());
        ir.getNodes().clear();
        int normalizedTables = 0;
        int recoveredTables = 0;
        ir.addDiagnostic(new Diagnostic("TABLE_RECOVER", "strategy", "normalize-existing-and-merge-adjacent-table-rows", System.currentTimeMillis()));

        for (int index = 0; index < oldNodes.size(); index++) {
            Node node = oldNodes.get(index);
            if (node.nodeType() == NodeType.TABLE) {
                ir.addNode(copyWithRecoveredTable(node, normalizeTableText(node.text())));
                normalizedTables++;
                continue;
            }

            if (isTableCandidateRow(node)) {
                List<Node> tableGroup = new ArrayList<>();
                tableGroup.add(node);
                int next = index + 1;
                while (next < oldNodes.size() && canJoinTableGroup(tableGroup.get(tableGroup.size() - 1), oldNodes.get(next))) {
                    tableGroup.add(oldNodes.get(next));
                    next++;
                }

                if (tableGroup.size() > 1) {
                    ir.addNode(buildRecoveredTable(tableGroup));
                    recoveredTables++;
                    index = next - 1;
                    continue;
                }
            }

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
                mergeTag(node.stageTags(), "TABLE_RECOVER")
            ));
        }
        ir.addDiagnostic(new Diagnostic("TABLE_RECOVER", "normalizedTables", normalizedTables, System.currentTimeMillis()));
        ir.addDiagnostic(new Diagnostic("TABLE_RECOVER", "recoveredTables", recoveredTables, System.currentTimeMillis()));
        ir.addDiagnostic(new Diagnostic("TABLE_RECOVER", "reason", normalizedTables == 0 && recoveredTables == 0 ? "no-table-candidates-found" : "table-normalization-or-recovery-applied", System.currentTimeMillis()));
    }

    private Node copyWithRecoveredTable(Node node, String normalizedText) {
        return new Node(
            node.id(),
            NodeType.TABLE,
            node.containerId(),
            node.bbox(),
            normalizedText,
            node.spans(),
            node.confidence(),
            node.orderKey(),
            node.sourceRefs(),
            node.properties(),
            mergeTag(node.stageTags(), "TABLE_RECOVER")
        );
    }

    private Node buildRecoveredTable(List<Node> tableGroup) {
        Node first = tableGroup.get(0);
        String tableText = normalizeTableText(joinTableRows(tableGroup));
        return new Node(
            UUID.randomUUID().toString(),
            NodeType.TABLE,
            first.containerId(),
            first.bbox(),
            tableText,
            List.of(),
            averageConfidence(tableGroup),
            first.orderKey(),
            mergeSourceRefs(tableGroup),
            mergeProperties(tableGroup),
            mergeStageTags(tableGroup)
        );
    }

    private String joinTableRows(List<Node> tableGroup) {
        List<String> rows = new ArrayList<>(tableGroup.size());
        for (Node node : tableGroup) {
            rows.add(node.text());
        }
        return String.join("\n", rows);
    }

    private double averageConfidence(List<Node> tableGroup) {
        double total = 0.0;
        for (Node node : tableGroup) {
            total += node.confidence();
        }
        return total / tableGroup.size();
    }

    private List<SourceRef> mergeSourceRefs(List<Node> tableGroup) {
        List<SourceRef> refs = new ArrayList<>();
        for (Node node : tableGroup) {
            refs.addAll(node.sourceRefs());
        }
        return refs;
    }

    private Map<String, Object> mergeProperties(List<Node> tableGroup) {
        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put("recoveredRowCount", tableGroup.size());
        Node first = tableGroup.get(0);
        merged.putAll(first.properties());
        return merged;
    }

    private Set<String> mergeStageTags(List<Node> tableGroup) {
        Set<String> merged = new LinkedHashSet<>();
        for (Node node : tableGroup) {
            merged.addAll(node.stageTags());
        }
        merged.add("TABLE_RECOVER");
        return merged;
    }

    private boolean canJoinTableGroup(Node previous, Node current) {
        return isTableCandidateRow(current)
            && java.util.Objects.equals(previous.containerId(), current.containerId())
            && matchesColumnCount(previous.text(), current.text());
    }

    private boolean isTableCandidateRow(Node node) {
        return node.nodeType() == NodeType.PARAGRAPH && countColumns(node.text()) >= 2;
    }

    private boolean matchesColumnCount(String left, String right) {
        return countColumns(left) == countColumns(right);
    }

    private int countColumns(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isBlank()) {
            return 0;
        }
        if (normalized.contains("|")) {
            return (int) java.util.Arrays.stream(normalized.split("\\|", -1))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .count();
        }
        if (normalized.contains("\t")) {
            return (int) java.util.Arrays.stream(normalized.split("\\t+"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .count();
        }
        return (int) java.util.Arrays.stream(normalized.split("\\s{2,}"))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .count();
    }

    private String normalizeTableText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        var rows = new ArrayList<String>();
        for (String line : text.replace("\r\n", "\n").split("\n")) {
            String cleaned = normalizeTableRow(line);
            if (!cleaned.isBlank()) {
                rows.add(cleaned);
            }
        }
        return String.join("\n", rows).trim();
    }

    private String normalizeTableRow(String row) {
        String trimmed = row.trim();
        if (trimmed.isBlank()) {
            return "";
        }
        if (trimmed.contains("|")) {
            String[] cells = trimmed.split("\\|", -1);
            List<String> normalizedCells = new ArrayList<>(cells.length);
            for (String cell : cells) {
                normalizedCells.add(cell.trim());
            }
            return String.join(" | ", normalizedCells).trim();
        }
        return trimmed.replace('\t', ' ').replaceAll(" {2,}", " ");
    }

    private java.util.Set<String> mergeTag(java.util.Set<String> tags, String tag) {
        var merged = new LinkedHashSet<>(tags);
        merged.add(tag);
        return merged;
    }
}
