package io.ycy.smartdocflow.layout;

import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.model.ir.Diagnostic;
import io.ycy.smartdocflow.core.model.ir.Node;
import io.ycy.smartdocflow.core.model.ir.NodeType;
import io.ycy.smartdocflow.core.spi.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;

public final class BasicNormalizer implements Normalizer {
    @Override
    public void normalize(DocumentIr ir) {
        var oldNodes = new ArrayList<>(ir.getNodes());
        ir.getNodes().clear();

        for (Node node : oldNodes) {
            String normalizedText = normalizeText(node.text(), node.nodeType());
            ir.addNode(new Node(
                node.id(),
                node.nodeType(),
                node.containerId(),
                node.bbox(),
                normalizedText,
                node.spans(),
                node.confidence(),
                node.orderKey(),
                node.sourceRefs(),
                node.properties(),
                mergeTag(node.stageTags(), "NORMALIZE")
            ));
        }
        ir.addDiagnostic(new Diagnostic("NORMALIZE", "normalizedNodes", oldNodes.size(), System.currentTimeMillis()));
    }

    private String normalizeText(String text, NodeType nodeType) {
        if (text == null) {
            return "";
        }

        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        var cleanedLines = new ArrayList<String>();
        boolean previousBlank = false;

        for (String line : lines) {
            String cleaned = nodeType == NodeType.TABLE
                ? line.stripTrailing()
                : collapseInlineWhitespace(line).strip();
            boolean blank = cleaned.isBlank();
            if (blank) {
                if (!previousBlank && !cleanedLines.isEmpty()) {
                    cleanedLines.add("");
                }
                previousBlank = true;
                continue;
            }
            cleanedLines.add(cleaned);
            previousBlank = false;
        }

        while (!cleanedLines.isEmpty() && cleanedLines.getFirst().isBlank()) {
            cleanedLines.removeFirst();
        }
        while (!cleanedLines.isEmpty() && cleanedLines.getLast().isBlank()) {
            cleanedLines.removeLast();
        }

        return String.join("\n", cleanedLines).trim();
    }

    private String collapseInlineWhitespace(String line) {
        return line.replace('\t', ' ').replaceAll(" {2,}", " ");
    }

    private java.util.Set<String> mergeTag(java.util.Set<String> tags, String tag) {
        var merged = new LinkedHashSet<>(tags);
        merged.add(tag);
        return merged;
    }
}
