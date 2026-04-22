package io.ycy.smartdocflow.core.pipeline;

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

public class PipelineBuilder {

    private DocumentDetector detector;
    private FormatExtractor extractor;
    private OcrProvider ocrProvider;
    private Normalizer normalizer;
    private Segmenter segmenter;
    private ReadingOrderResolver orderResolver;
    private BlockClassifier classifier;
    private TableRecoverer tableRecoverer;
    private StructureRepairer repairer;
    private PostProcessor postProcessor;
    private ResultRenderer renderer;

    public PipelineBuilder detector(DocumentDetector detector) {
        this.detector = detector;
        return this;
    }

    public PipelineBuilder extractor(FormatExtractor extractor) {
        this.extractor = extractor;
        return this;
    }

    public PipelineBuilder ocrProvider(OcrProvider ocrProvider) {
        this.ocrProvider = ocrProvider;
        return this;
    }

    public PipelineBuilder normalizer(Normalizer normalizer) {
        this.normalizer = normalizer;
        return this;
    }

    public PipelineBuilder segmenter(Segmenter segmenter) {
        this.segmenter = segmenter;
        return this;
    }

    public PipelineBuilder orderResolver(ReadingOrderResolver orderResolver) {
        this.orderResolver = orderResolver;
        return this;
    }

    public PipelineBuilder classifier(BlockClassifier classifier) {
        this.classifier = classifier;
        return this;
    }

    public PipelineBuilder tableRecoverer(TableRecoverer tableRecoverer) {
        this.tableRecoverer = tableRecoverer;
        return this;
    }

    public PipelineBuilder repairer(StructureRepairer repairer) {
        this.repairer = repairer;
        return this;
    }

    public PipelineBuilder postProcessor(PostProcessor postProcessor) {
        this.postProcessor = postProcessor;
        return this;
    }

    public PipelineBuilder renderer(ResultRenderer renderer) {
        this.renderer = renderer;
        return this;
    }

    public Pipeline build() {
        return new Pipeline(
            detector,
            extractor,
            ocrProvider,
            normalizer,
            segmenter,
            orderResolver,
            classifier,
            tableRecoverer,
            repairer,
            postProcessor,
            renderer
        );
    }
}
