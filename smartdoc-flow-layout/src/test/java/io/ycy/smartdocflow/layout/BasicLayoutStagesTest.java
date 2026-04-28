package io.ycy.smartdocflow.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.ycy.smartdocflow.common.model.Bbox;
import io.ycy.smartdocflow.common.model.DocumentSourceType;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.model.ir.Diagnostic;
import io.ycy.smartdocflow.core.model.ir.DocumentMeta;
import io.ycy.smartdocflow.core.model.ir.Node;
import io.ycy.smartdocflow.core.model.ir.NodeType;
import io.ycy.smartdocflow.core.model.ir.SourceRef;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BasicLayoutStagesTest {
    private final BasicNormalizer normalizer = new BasicNormalizer();
    private final BasicSegmenter segmenter = new BasicSegmenter();
    private final BasicReadingOrderResolver orderResolver = new BasicReadingOrderResolver();
    private final BasicClassifier classifier = new BasicClassifier();
    private final BasicTableRecoverer tableRecoverer = new BasicTableRecoverer();
    private final BasicRepairer repairer = new BasicRepairer();
    private final NoopPostProcessor postProcessor = new NoopPostProcessor();

    @Test
    void normalizerCollapsesWhitespaceForParagraphs() {
        DocumentIr ir = createIr();
        ir.addNode(new Node(
            "node-1",
            NodeType.PARAGRAPH,
            "page-1",
            Bbox.EMPTY,
            "  First\tline  \r\n\r\n   Second   line   ",
            List.of(),
            1.0,
            "0",
            List.of(new SourceRef("test", "page-1", 0, 10)),
            Map.of(),
            Set.of("EXTRACT")
        ));

        normalizer.normalize(ir);

        assertEquals(1, ir.getNodes().size());
        assertEquals("First line\n\nSecond line", ir.getNodes().getFirst().text());
        assertTrue(ir.getNodes().getFirst().stageTags().contains("NORMALIZE"));
        assertTrue(hasDiagnostic(ir, "NORMALIZE", "normalizedNodes"));
        assertTrue(hasDiagnostic(ir, "NORMALIZE", "strategy"));
        assertTrue(hasDiagnostic(ir, "NORMALIZE", "reason"));
    }

    @Test
    void segmenterSplitsParagraphsByBlankLines() {
        DocumentIr ir = createIr();
        ir.addNode(new Node(
            "node-1",
            NodeType.PARAGRAPH,
            "page-1",
            Bbox.EMPTY,
            "Part one\n\nPart two\n\nPart three",
            List.of(),
            1.0,
            "0",
            List.of(new SourceRef("test", "page-1", 0, 30)),
            Map.of(),
            Set.of("NORMALIZE")
        ));

        segmenter.segment(ir);

        assertEquals(3, ir.getNodes().size());
        assertEquals("Part one", ir.getNodes().get(0).text());
        assertEquals("Part two", ir.getNodes().get(1).text());
        assertEquals("Part three", ir.getNodes().get(2).text());
        assertEquals("0.0", ir.getNodes().get(0).orderKey());
        assertEquals("0.1", ir.getNodes().get(1).orderKey());
        assertEquals("0.2", ir.getNodes().get(2).orderKey());
        assertTrue(ir.getNodes().stream().allMatch(node -> node.stageTags().contains("SEGMENT")));
        assertTrue(hasDiagnostic(ir, "SEGMENT", "createdSegments"));
        assertTrue(hasDiagnostic(ir, "SEGMENT", "strategy"));
        assertTrue(hasDiagnostic(ir, "SEGMENT", "reason"));
    }

    @Test
    void segmenterLeavesTablesUntouched() {
        DocumentIr ir = createIr();
        ir.addNode(new Node(
            "node-1",
            NodeType.TABLE,
            "page-1",
            Bbox.EMPTY,
            "Col1 | Col2\n\nA | B",
            List.of(),
            1.0,
            "0",
            List.of(new SourceRef("test", "page-1", 0, 18)),
            Map.of(),
            Set.of("NORMALIZE")
        ));

        segmenter.segment(ir);

        assertEquals(1, ir.getNodes().size());
        assertEquals("Col1 | Col2\n\nA | B", ir.getNodes().getFirst().text());
        assertTrue(ir.getNodes().getFirst().stageTags().contains("SEGMENT"));
    }

    @Test
    void orderResolverSortsByContainerAndOrderKey() {
        DocumentIr ir = createIr();
        ir.addContainer(new io.ycy.smartdocflow.core.model.ir.Container("page-2", io.ycy.smartdocflow.core.model.ir.ContainerType.PAGE, 1, "Page 2", 0, 0, Bbox.EMPTY, Map.of()));
        ir.addContainer(new io.ycy.smartdocflow.core.model.ir.Container("page-1", io.ycy.smartdocflow.core.model.ir.ContainerType.PAGE, 0, "Page 1", 0, 0, Bbox.EMPTY, Map.of()));

        ir.addNode(new Node("node-3", NodeType.PARAGRAPH, "page-2", Bbox.EMPTY, "page two", List.of(), 1.0, "0.0", List.of(), Map.of(), Set.of("SEGMENT")));
        ir.addNode(new Node("node-2", NodeType.PARAGRAPH, "page-1", Bbox.EMPTY, "later", List.of(), 1.0, "0.1", List.of(), Map.of(), Set.of("SEGMENT")));
        ir.addNode(new Node("node-1", NodeType.PARAGRAPH, "page-1", Bbox.EMPTY, "first", List.of(), 1.0, "0.0", List.of(), Map.of(), Set.of("SEGMENT")));

        orderResolver.resolve(ir);

        assertEquals(3, ir.getNodes().size());
        assertEquals("first", ir.getNodes().get(0).text());
        assertEquals("later", ir.getNodes().get(1).text());
        assertEquals("page two", ir.getNodes().get(2).text());
        assertTrue(ir.getNodes().stream().allMatch(node -> node.stageTags().contains("ORDER")));
        assertTrue(hasDiagnostic(ir, "ORDER", "orderedNodes"));
        assertTrue(hasDiagnostic(ir, "ORDER", "strategy"));
        assertTrue(hasDiagnostic(ir, "ORDER", "reason"));
    }

    @Test
    void postProcessorRemovesBlankAndAdjacentDuplicateNodes() {
        DocumentIr ir = createIr();
        ir.addNode(new Node("node-1", NodeType.PARAGRAPH, "page-1", Bbox.EMPTY, "  kept  ", List.of(), 1.0, "0", List.of(), Map.of(), Set.of("ORDER")));
        ir.addNode(new Node("node-2", NodeType.PARAGRAPH, "page-1", Bbox.EMPTY, "kept", List.of(), 1.0, "1", List.of(), Map.of(), Set.of("ORDER")));
        ir.addNode(new Node("node-3", NodeType.PARAGRAPH, "page-1", Bbox.EMPTY, "   ", List.of(), 1.0, "2", List.of(), Map.of(), Set.of("ORDER")));
        ir.addNode(new Node("node-4", NodeType.PARAGRAPH, "page-2", Bbox.EMPTY, "kept", List.of(), 1.0, "3", List.of(), Map.of(), Set.of("ORDER")));

        postProcessor.process(ir);

        assertEquals(2, ir.getNodes().size());
        assertEquals("kept", ir.getNodes().get(0).text());
        assertEquals("page-1", ir.getNodes().get(0).containerId());
        assertEquals("page-2", ir.getNodes().get(1).containerId());
        assertTrue(ir.getNodes().stream().allMatch(node -> node.stageTags().contains("POST")));
        assertTrue(hasDiagnostic(ir, "POST", "removedBlankNodes"));
        assertTrue(hasDiagnostic(ir, "POST", "removedDuplicateNodes"));
        assertTrue(hasDiagnostic(ir, "POST", "strategy"));
        assertTrue(hasDiagnostic(ir, "POST", "reason"));
    }

    @Test
    void tableRecovererNormalizesBasicTableRows() {
        DocumentIr ir = createIr();
        ir.addNode(new Node(
            "node-1",
            NodeType.TABLE,
            "page-1",
            Bbox.EMPTY,
            " Col1|Col2 \n\n A | B  \n C|  D ",
            List.of(),
            1.0,
            "0",
            List.of(new SourceRef("test", "page-1", 0, 20)),
            Map.of(),
            Set.of("CLASSIFY")
        ));

        tableRecoverer.recover(ir);

        assertEquals(1, ir.getNodes().size());
        assertEquals(NodeType.TABLE, ir.getNodes().getFirst().nodeType());
        assertEquals("Col1 | Col2\nA | B\nC | D", ir.getNodes().getFirst().text());
        assertTrue(ir.getNodes().getFirst().stageTags().contains("TABLE_RECOVER"));
        assertTrue(hasDiagnostic(ir, "TABLE_RECOVER", "normalizedTables"));
        assertTrue(hasDiagnostic(ir, "TABLE_RECOVER", "strategy"));
        assertTrue(hasDiagnostic(ir, "TABLE_RECOVER", "reason"));
    }

    @Test
    void tableRecovererMergesAdjacentParagraphRowsIntoTable() {
        DocumentIr ir = createIr();
        ir.addNode(new Node("node-1", NodeType.PARAGRAPH, "page-1", Bbox.EMPTY, "Name | Score | Rank", List.of(), 1.0, "0.0", List.of(), Map.of(), Set.of("CLASSIFY")));
        ir.addNode(new Node("node-2", NodeType.PARAGRAPH, "page-1", Bbox.EMPTY, "Alice | 98 | 1", List.of(), 1.0, "0.1", List.of(), Map.of(), Set.of("CLASSIFY")));
        ir.addNode(new Node("node-3", NodeType.PARAGRAPH, "page-1", Bbox.EMPTY, "Bob | 91 | 2", List.of(), 1.0, "0.2", List.of(), Map.of(), Set.of("CLASSIFY")));
        ir.addNode(new Node("node-4", NodeType.PARAGRAPH, "page-1", Bbox.EMPTY, "Tail paragraph", List.of(), 1.0, "0.3", List.of(), Map.of(), Set.of("CLASSIFY")));

        tableRecoverer.recover(ir);

        assertEquals(2, ir.getNodes().size());
        assertEquals(NodeType.TABLE, ir.getNodes().get(0).nodeType());
        assertEquals("Name | Score | Rank\nAlice | 98 | 1\nBob | 91 | 2", ir.getNodes().get(0).text());
        assertEquals("Tail paragraph", ir.getNodes().get(1).text());
        assertEquals(3, assertInstanceOf(Integer.class, ir.getNodes().get(0).properties().get("recoveredRowCount")));
        assertTrue(hasDiagnostic(ir, "TABLE_RECOVER", "recoveredTables"));
    }

    @Test
    void classifierRecordsHeuristicDiagnostics() {
        DocumentIr ir = createIr();
        ir.addNode(new Node("node-1", NodeType.PARAGRAPH, "page-1", Bbox.EMPTY, "Executive Summary", List.of(), 1.0, "0.0", List.of(), Map.of(), Set.of("ORDER")));
        ir.addNode(new Node("node-2", NodeType.PARAGRAPH, "page-1", Bbox.EMPTY, "Name | Score | Rank", List.of(), 1.0, "0.1", List.of(), Map.of(), Set.of("ORDER")));

        classifier.classify(ir);

        assertTrue(hasDiagnostic(ir, "CLASSIFY", "strategy"));
        assertTrue(hasDiagnostic(ir, "CLASSIFY", "headingCandidates"));
        assertTrue(hasDiagnostic(ir, "CLASSIFY", "tableCandidates"));
        assertTrue(hasDiagnostic(ir, "CLASSIFY", "reason"));
    }

    @Test
    void repairerRecordsRepairDiagnostics() {
        DocumentIr ir = createIr();
        ir.addNode(new Node("node-1", NodeType.PARAGRAPH, "page-1", Bbox.EMPTY, "First line", List.of(), 1.0, "0", List.of(), Map.of(), Set.of("TABLE_RECOVER")));
        ir.addNode(new Node("node-2", NodeType.PARAGRAPH, "page-1", Bbox.EMPTY, "continues here", List.of(), 1.0, "1", List.of(), Map.of(), Set.of("TABLE_RECOVER")));
        ir.addNode(new Node("node-3", NodeType.HEADER, "page-1", Bbox.EMPTY, "Header", List.of(), 1.0, "2", List.of(), Map.of(), Set.of("TABLE_RECOVER")));

        repairer.repair(ir);

        assertTrue(hasDiagnostic(ir, "REPAIR", "strategy"));
        assertTrue(hasDiagnostic(ir, "REPAIR", "mergedParagraphs"));
        assertTrue(hasDiagnostic(ir, "REPAIR", "skippedStructuralNodes"));
        assertTrue(hasDiagnostic(ir, "REPAIR", "reason"));
    }

    private boolean hasDiagnostic(DocumentIr ir, String stage, String key) {
        return ir.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.stage().equals(stage) && diagnostic.key().equals(key));
    }

    private DocumentIr createIr() {
        return new DocumentIr(new DocumentMeta(
            "doc-1",
            DocumentSourceType.PDF,
            "sample.pdf",
            1,
            List.of(),
            false,
            false,
            false,
            false,
            "test",
            "test"
        ));
    }
}
