package io.ycy.smartdocflow.layout;

import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.model.ir.Diagnostic;
import io.ycy.smartdocflow.core.model.ir.Node;
import io.ycy.smartdocflow.core.model.ir.NodeType;
import io.ycy.smartdocflow.core.spi.TableRecoverer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class BasicTableRecoverer implements TableRecoverer {
    @Override
    public void recover(DocumentIr ir) {
        var oldNodes = new ArrayList<>(ir.getNodes());
        ir.getNodes().clear();
        int normalizedTables = 0;

        for (Node node : oldNodes) {
            if (node.nodeType() == NodeType.TABLE) {
                ir.addNode(new Node(
                    node.id(),
                    NodeType.TABLE,
                    node.containerId(),
                    node.bbox(),
                    normalizeTableText(node.text()),
                    node.spans(),
                    node.confidence(),
                    node.orderKey(),
                    node.sourceRefs(),
                    node.properties(),
                    mergeTag(node.stageTags(), "TABLE_RECOVER")
                ));
                normalizedTables++;
                continue;
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
