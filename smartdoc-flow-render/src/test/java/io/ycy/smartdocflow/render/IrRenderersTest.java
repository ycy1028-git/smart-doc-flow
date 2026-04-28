package io.ycy.smartdocflow.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.ycy.smartdocflow.common.model.Bbox;
import io.ycy.smartdocflow.common.model.DocumentSourceType;
import io.ycy.smartdocflow.core.model.ParseOptions;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.model.ir.DocumentMeta;
import io.ycy.smartdocflow.core.model.ir.Node;
import io.ycy.smartdocflow.core.model.ir.NodeType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IrRenderersTest {
    @Test
    void markdownRendererUsesHeadingAndFencedTableRules() {
        DocumentIr ir = createIr();
        ir.addNode(new Node("n1", NodeType.TITLE, "page-1", Bbox.EMPTY, "Quarterly\nReview", List.of(), 1.0, "0", List.of(), Map.of(), Set.of("EXTRACT")));
        ir.addNode(new Node("n2", NodeType.TABLE, "page-1", Bbox.EMPTY, "\nRegion | Revenue\r\nAPAC | 320\n", List.of(), 1.0, "1", List.of(), Map.of(), Set.of("EXTRACT")));

        String markdown = new IrMarkdownRenderer().render(ir, ParseOptions.markdown());

        assertTrue(markdown.startsWith("# quarterly-review.pptx"));
        assertTrue(markdown.contains("## Quarterly Review"));
        assertTrue(markdown.contains("```text"));
        assertTrue(markdown.contains("Region | Revenue"));
        assertTrue(markdown.contains("APAC | 320"));
    }

    @Test
    void jsonRendererEscapesControlCharacters() {
        DocumentIr ir = createIr();
        ir.addNode(new Node("n1", NodeType.PARAGRAPH, "page-1", Bbox.EMPTY, "Line 1\n\"quoted\"\\path", List.of(), 1.0, "0", List.of(), Map.of(), Set.of("EXTRACT")));

        String json = new IrJsonRenderer().render(ir, ParseOptions.json());

        assertTrue(json.contains("\"documentId\":\"doc-1\""));
        assertTrue(json.contains("\"fileName\":\"quarterly-review.pptx\""));
        assertTrue(json.contains("Line 1\\n\\\"quoted\\\"\\\\path"));
    }

    @Test
    void jsonRendererNormalizesNullTextToEmptyString() {
        DocumentIr ir = createIr();
        ir.addNode(new Node("n1", NodeType.PARAGRAPH, "page-1", Bbox.EMPTY, null, List.of(), 1.0, "0", List.of(), Map.of(), Set.of("EXTRACT")));

        String json = new IrJsonRenderer().render(ir, ParseOptions.json());

        assertTrue(json.contains("\"text\":\"\""));
        assertEquals(1, countEmptyTextOccurrences(json));
    }

    private int countEmptyTextOccurrences(String value) {
        String token = "\"text\":\"\"";
        int count = 0;
        int fromIndex = 0;
        while ((fromIndex = value.indexOf(token, fromIndex)) >= 0) {
            count++;
            fromIndex += token.length();
        }
        return count;
    }

    private DocumentIr createIr() {
        return new DocumentIr(new DocumentMeta(
            "doc-1",
            DocumentSourceType.PPTX,
            "quarterly-review.pptx",
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
