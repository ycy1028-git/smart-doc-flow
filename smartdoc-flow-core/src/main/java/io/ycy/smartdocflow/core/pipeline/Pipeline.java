package io.ycy.smartdocflow.core.pipeline;

import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.model.ParseOptions;
import io.ycy.smartdocflow.core.model.ir.Diagnostic;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.model.ir.DocumentMeta;
import io.ycy.smartdocflow.core.spi.BlockClassifier;
import io.ycy.smartdocflow.core.spi.DocumentDetector;
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
import java.util.UUID;

public class Pipeline {

    private final DocumentDetector detector;
    private final FormatExtractor extractor;
    private final OcrProvider ocrProvider;
    private final Normalizer normalizer;
    private final Segmenter segmenter;
    private final ReadingOrderResolver orderResolver;
    private final BlockClassifier classifier;
    private final TableRecoverer tableRecoverer;
    private final StructureRepairer repairer;
    private final PostProcessor postProcessor;
    private final ResultRenderer renderer;

    Pipeline(
        DocumentDetector detector,
        FormatExtractor extractor,
        OcrProvider ocrProvider,
        Normalizer normalizer,
        Segmenter segmenter,
        ReadingOrderResolver orderResolver,
        BlockClassifier classifier,
        TableRecoverer tableRecoverer,
        StructureRepairer repairer,
        PostProcessor postProcessor,
        ResultRenderer renderer
    ) {
        this.detector = detector;
        this.extractor = extractor;
        this.ocrProvider = ocrProvider;
        this.normalizer = normalizer;
        this.segmenter = segmenter;
        this.orderResolver = orderResolver;
        this.classifier = classifier;
        this.tableRecoverer = tableRecoverer;
        this.repairer = repairer;
        this.postProcessor = postProcessor;
        this.renderer = renderer;
    }

    public DocumentProfile detect(Path source) {
        return detector.detect(source);
    }

    public DocumentIr execute(Path source) {
        DocumentProfile profile = detector.detect(source);

        DocumentIr ir = new DocumentIr(new DocumentMeta(
            UUID.randomUUID().toString(),
            profile.sourceType(),
            source.getFileName().toString(),
            0,
            java.util.List.of(),
            profile.scanned(),
            profile.tableHeavy(),
            profile.imageHeavy(),
            profile.multiColumn(),
            null,
            null
        ));

        addDiagnostic(ir, "PIPELINE", "sourceType", profile.sourceType().name());
        addDiagnostic(ir, "PIPELINE", "scanned", profile.scanned());
        addDiagnostic(ir, "PIPELINE", "tableHeavy", profile.tableHeavy());
        addDiagnostic(ir, "PIPELINE", "imageHeavy", profile.imageHeavy());
        addDiagnostic(ir, "PIPELINE", "multiColumn", profile.multiColumn());

        runStage(ir, "EXTRACT", () -> extractor.extract(source, profile, ir));
        runStage(ir, "OCR", () -> ocrProvider.process(source, ir, profile));
        runStage(ir, "NORMALIZE", () -> normalizer.normalize(ir));
        runStage(ir, "SEGMENT", () -> segmenter.segment(ir));
        runStage(ir, "ORDER", () -> orderResolver.resolve(ir));
        runStage(ir, "CLASSIFY", () -> classifier.classify(ir));
        runStage(ir, "TABLE_RECOVER", () -> tableRecoverer.recover(ir));
        runStage(ir, "REPAIR", () -> repairer.repair(ir));
        runStage(ir, "POST", () -> postProcessor.process(ir));

        return ir;
    }

    public String render(DocumentIr ir, ParseOptions options) {
        return renderer.render(ir, options);
    }

    private void runStage(DocumentIr ir, String stage, Runnable action) {
        long startedAt = System.currentTimeMillis();
        int beforeNodes = ir.getNodes().size();
        recordStageStart(ir, stage, startedAt, beforeNodes);
        try {
            action.run();
            recordStageEnd(ir, stage, startedAt, beforeNodes);
        } catch (RuntimeException e) {
            addDiagnostic(ir, stage, "failed", true);
            addDiagnostic(ir, stage, "error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getClass().getSimpleName() + ": " + e.getMessage());
            recordStageEnd(ir, stage, startedAt, beforeNodes);
            throw e;
        }
    }

    private void recordStageStart(DocumentIr ir, String stage, long startedAt, int beforeNodes) {
        addDiagnostic(ir, stage, "started", true);
        addDiagnostic(ir, stage, "startedAt", startedAt);
        addDiagnostic(ir, stage, "beforeNodes", beforeNodes);
    }

    private void recordStageStart(DocumentIr ir, String stage) {
        addDiagnostic(ir, stage, "beforeNodes", ir.getNodes().size());
    }

    private void recordStageEnd(DocumentIr ir, String stage, long startedAt, int beforeNodes) {
        addDiagnostic(ir, stage, "completed", true);
        addDiagnostic(ir, stage, "durationMs", Math.max(0, System.currentTimeMillis() - startedAt));
        addDiagnostic(ir, stage, "afterNodes", ir.getNodes().size());
        addDiagnostic(ir, stage, "nodeDelta", ir.getNodes().size() - beforeNodes);
    }

    private void addDiagnostic(DocumentIr ir, String stage, String key, Object value) {
        ir.addDiagnostic(new Diagnostic(stage, key, value, System.currentTimeMillis()));
    }
}
