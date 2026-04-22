package io.ycy.smartdocflow.core.pipeline;

import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.model.ParseOptions;
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

        extractor.extract(source, profile, ir);
        ocrProvider.process(ir, profile);
        normalizer.normalize(ir);
        segmenter.segment(ir);
        orderResolver.resolve(ir);
        classifier.classify(ir);
        tableRecoverer.recover(ir);
        repairer.repair(ir);
        postProcessor.process(ir);

        return ir;
    }

    public String render(DocumentIr ir, ParseOptions options) {
        return renderer.render(ir, options);
    }
}
