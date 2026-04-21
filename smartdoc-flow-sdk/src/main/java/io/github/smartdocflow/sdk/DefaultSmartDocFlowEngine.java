package io.github.smartdocflow.sdk;

import io.github.smartdocflow.core.SmartDocFlowEngine;
import io.github.smartdocflow.core.model.DocumentProfile;
import io.github.smartdocflow.core.model.DocumentResult;
import io.github.smartdocflow.core.model.OutputFormat;
import io.github.smartdocflow.core.model.ParseOptions;
import io.github.smartdocflow.core.spi.DocumentProfiler;
import io.github.smartdocflow.core.spi.FormatExtractor;
import io.github.smartdocflow.core.spi.LayoutAnalyzer;
import io.github.smartdocflow.core.spi.Renderer;
import io.github.smartdocflow.core.spi.OcrProcessor;
import io.github.smartdocflow.format.BasicDocumentProfiler;
import io.github.smartdocflow.format.BasicFormatExtractor;
import io.github.smartdocflow.layout.BasicLayoutAnalyzer;
import io.github.smartdocflow.ocr.NoopOcrProcessor;
import io.github.smartdocflow.render.JsonRenderer;
import io.github.smartdocflow.render.MarkdownRenderer;
import java.nio.file.Path;

public final class DefaultSmartDocFlowEngine implements SmartDocFlowEngine {
    private final DocumentProfiler profiler;
    private final FormatExtractor extractor;
    private final OcrProcessor ocrProcessor;
    private final LayoutAnalyzer layoutAnalyzer;
    private final Renderer markdownRenderer;
    private final Renderer jsonRenderer;

    public DefaultSmartDocFlowEngine() {
        this(
            new BasicDocumentProfiler(),
            new BasicFormatExtractor(),
            new NoopOcrProcessor(),
            new BasicLayoutAnalyzer(),
            new MarkdownRenderer(),
            new JsonRenderer()
        );
    }

    public DefaultSmartDocFlowEngine(
        DocumentProfiler profiler,
        FormatExtractor extractor,
        OcrProcessor ocrProcessor,
        LayoutAnalyzer layoutAnalyzer,
        Renderer markdownRenderer,
        Renderer jsonRenderer
    ) {
        this.profiler = profiler;
        this.extractor = extractor;
        this.ocrProcessor = ocrProcessor;
        this.layoutAnalyzer = layoutAnalyzer;
        this.markdownRenderer = markdownRenderer;
        this.jsonRenderer = jsonRenderer;
    }

    @Override
    public DocumentProfile profile(Path source) {
        return profiler.profile(source);
    }

    @Override
    public DocumentResult parse(Path source, ParseOptions options) {
        DocumentProfile profile = profile(source);
        var extractionResult = extractor.extract(source, profile);
        var ocrResult = ocrProcessor.process(extractionResult, profile);
        return layoutAnalyzer.analyze(extractionResult, ocrResult, profile).documentResult();
    }

    @Override
    public String render(Path source, ParseOptions options) {
        DocumentResult result = parse(source, options);
        return rendererFor(options.outputFormat()).render(result);
    }

    private Renderer rendererFor(OutputFormat outputFormat) {
        return outputFormat == OutputFormat.JSON ? jsonRenderer : markdownRenderer;
    }
}
