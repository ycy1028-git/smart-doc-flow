package io.ycy.smartdocflow.core.pipeline;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.ycy.smartdocflow.common.model.DocumentSourceType;
import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.model.ir.Node;
import io.ycy.smartdocflow.core.model.ir.NodeType;
import io.ycy.smartdocflow.core.spi.BlockClassifier;
import io.ycy.smartdocflow.core.spi.DocumentDetector;
import io.ycy.smartdocflow.core.spi.FormatExtractionResult;
import io.ycy.smartdocflow.core.spi.FormatExtractor;
import io.ycy.smartdocflow.core.spi.Normalizer;
import io.ycy.smartdocflow.core.spi.OcrProvider;
import io.ycy.smartdocflow.core.spi.PostProcessor;
import io.ycy.smartdocflow.core.spi.ReadingOrderResolver;
import io.ycy.smartdocflow.core.spi.ResultRenderer;
import io.ycy.smartdocflow.core.spi.Segmenter;
import io.ycy.smartdocflow.core.spi.StructureRepairer;
import io.ycy.smartdocflow.core.spi.TableRecoverer;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PipelineDiagnosticsTest {

    @Test
    void pipelineRecordsStageDiagnostics() {
        Pipeline pipeline = new PipelineBuilder()
            .detector(source -> new DocumentProfile(DocumentSourceType.PDF, false, false, false, false))
            .extractor(new TestExtractor())
            .ocrProvider((source, ir, profile) -> { })
            .normalizer(ir -> { })
            .segmenter(ir -> { })
            .orderResolver(ir -> { })
            .classifier(ir -> { })
            .tableRecoverer(ir -> { })
            .repairer(ir -> { })
            .postProcessor(ir -> { })
            .renderer((ir, options) -> "")
            .build();

        DocumentIr ir = pipeline.execute(Path.of("sample.pdf"));

        assertTrue(hasDiagnostic(ir, "PIPELINE", "sourceType"));
        assertTrue(hasDiagnostic(ir, "PIPELINE", "scanned"));
        assertTrue(hasDiagnostic(ir, "EXTRACT", "beforeNodes"));
        assertTrue(hasDiagnostic(ir, "EXTRACT", "afterNodes"));
        assertTrue(hasDiagnostic(ir, "OCR", "beforeNodes"));
        assertTrue(hasDiagnostic(ir, "POST", "afterNodes"));
    }

    private boolean hasDiagnostic(DocumentIr ir, String stage, String key) {
        return ir.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.stage().equals(stage) && diagnostic.key().equals(key));
    }

    private static final class TestExtractor implements FormatExtractor {
        @Override
        public FormatExtractionResult extract(Path source, DocumentProfile profile) {
            return new FormatExtractionResult(source, "text", List.of("text"), List.of(List.of("text")), 1);
        }

        @Override
        public void extract(Path source, DocumentProfile profile, DocumentIr ir) {
            ir.addNode(new Node(
                "node-1",
                NodeType.PARAGRAPH,
                null,
                io.ycy.smartdocflow.common.model.Bbox.EMPTY,
                "text",
                List.of(),
                1.0,
                "0",
                List.of(),
                java.util.Map.of(),
                java.util.Set.of("EXTRACT")
            ));
        }
    }
}
