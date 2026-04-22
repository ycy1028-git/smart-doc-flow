package io.ycy.smartdocflow.sdk;

import io.ycy.smartdocflow.core.SmartDocFlowEngine;
import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.model.DocumentResult;
import io.ycy.smartdocflow.core.model.OutputFormat;
import io.ycy.smartdocflow.core.model.ParseOptions;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.pipeline.IrResultMapper;
import io.ycy.smartdocflow.core.pipeline.Pipeline;
import io.ycy.smartdocflow.core.pipeline.PipelineBuilder;
import io.ycy.smartdocflow.format.BasicDocumentProfiler;
import io.ycy.smartdocflow.format.BasicFormatExtractor;
import io.ycy.smartdocflow.format.BasicOfficeExtractor;
import io.ycy.smartdocflow.format.BasicPdfExtractor;
import io.ycy.smartdocflow.layout.BasicClassifier;
import io.ycy.smartdocflow.layout.BasicNormalizer;
import io.ycy.smartdocflow.layout.BasicReadingOrderResolver;
import io.ycy.smartdocflow.layout.BasicRepairer;
import io.ycy.smartdocflow.layout.BasicSegmenter;
import io.ycy.smartdocflow.layout.BasicTableRecoverer;
import io.ycy.smartdocflow.layout.NoopPostProcessor;
import io.ycy.smartdocflow.ocr.BasicOcrProvider;
import io.ycy.smartdocflow.render.IrJsonRenderer;
import io.ycy.smartdocflow.render.IrMarkdownRenderer;
import io.ycy.smartdocflow.core.spi.FormatExtractionResult;
import io.ycy.smartdocflow.core.spi.FormatExtractor;
import io.ycy.smartdocflow.common.model.DocumentSourceType;
import java.nio.file.Path;

public final class DefaultSmartDocFlowEngine implements SmartDocFlowEngine {
    private final Pipeline pipeline;
    private final IrMarkdownRenderer markdownRenderer;
    private final IrJsonRenderer jsonRenderer;

    public DefaultSmartDocFlowEngine() {
        this(defaultPipeline(), new IrMarkdownRenderer(), new IrJsonRenderer());
    }

    public DefaultSmartDocFlowEngine(Pipeline pipeline) {
        this(pipeline, new IrMarkdownRenderer(), new IrJsonRenderer());
    }

    public DefaultSmartDocFlowEngine(Pipeline pipeline, IrMarkdownRenderer markdownRenderer, IrJsonRenderer jsonRenderer) {
        this.pipeline = pipeline;
        this.markdownRenderer = markdownRenderer;
        this.jsonRenderer = jsonRenderer;
    }

    @Override
    public DocumentProfile profile(Path source) {
        return pipeline.detect(source);
    }

    @Override
    public DocumentResult parse(Path source, ParseOptions options) {
        DocumentIr ir = pipeline.execute(source);
        return IrResultMapper.toDocumentResult(ir);
    }

    @Override
    public String render(Path source, ParseOptions options) {
        DocumentIr ir = pipeline.execute(source);
        return rendererFor(options.outputFormat()).render(ir, options);
    }

    private static Pipeline defaultPipeline() {
        BasicDocumentProfiler detector = new BasicDocumentProfiler();
        FormatExtractor extractor = new CompositeFormatExtractor(
            new BasicPdfExtractor(),
            new BasicOfficeExtractor(),
            new BasicFormatExtractor()
        );

        return new PipelineBuilder()
            .detector(detector)
            .extractor(extractor)
            .ocrProvider(new BasicOcrProvider())
            .normalizer(new BasicNormalizer())
            .segmenter(new BasicSegmenter())
            .orderResolver(new BasicReadingOrderResolver())
            .classifier(new BasicClassifier())
            .tableRecoverer(new BasicTableRecoverer())
            .repairer(new BasicRepairer())
            .postProcessor(new NoopPostProcessor())
            .renderer(new IrMarkdownRenderer())
            .build();
    }

    private io.ycy.smartdocflow.core.spi.ResultRenderer rendererFor(OutputFormat outputFormat) {
        return outputFormat == OutputFormat.JSON ? jsonRenderer : markdownRenderer;
    }

    private static final class CompositeFormatExtractor implements FormatExtractor {
        private final BasicPdfExtractor pdfExtractor;
        private final BasicOfficeExtractor officeExtractor;
        private final BasicFormatExtractor fallbackExtractor;

        private CompositeFormatExtractor(
            BasicPdfExtractor pdfExtractor,
            BasicOfficeExtractor officeExtractor,
            BasicFormatExtractor fallbackExtractor
        ) {
            this.pdfExtractor = pdfExtractor;
            this.officeExtractor = officeExtractor;
            this.fallbackExtractor = fallbackExtractor;
        }

        @Override
        public FormatExtractionResult extract(Path source, DocumentProfile profile) {
            return switch (profile.sourceType()) {
                case PDF -> pdfExtractor.extract(source, profile);
                case DOCX, XLSX, PPTX -> officeExtractor.extract(source, profile);
                case IMAGE, UNKNOWN -> fallbackExtractor.extract(source, profile);
            };
        }

        @Override
        public void extract(Path source, DocumentProfile profile, DocumentIr ir) {
            switch (profile.sourceType()) {
                case PDF -> pdfExtractor.extract(source, profile, ir);
                case DOCX, XLSX, PPTX -> officeExtractor.extract(source, profile, ir);
                case IMAGE, UNKNOWN -> fallbackExtractor.extract(source, profile, ir);
            }
        }
    }
}
