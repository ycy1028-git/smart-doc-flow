package io.ycy.smartdocflow.ocr;

import io.ycy.smartdocflow.common.model.Bbox;
import io.ycy.smartdocflow.common.model.DocumentSourceType;
import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.model.ir.Container;
import io.ycy.smartdocflow.core.model.ir.ContainerType;
import io.ycy.smartdocflow.core.model.ir.Diagnostic;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.model.ir.Node;
import io.ycy.smartdocflow.core.model.ir.NodeType;
import io.ycy.smartdocflow.core.model.ir.SourceRef;
import io.ycy.smartdocflow.core.spi.OcrProvider;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

public final class BasicOcrProvider implements OcrProvider {
    private static final String DEFAULT_OCR_LANG = "chi_sim+eng";

    @Override
    public void process(Path source, DocumentIr ir, DocumentProfile profile) {
        if (!requiresOcr(profile)) {
            addDiagnostic(ir, "decision", "skip-ocr");
            addDiagnostic(ir, "reason", "ocr-not-required");
            return;
        }

        addDiagnostic(ir, "decision", "run-ocr");
        addDiagnostic(ir, "reason", profile.sourceType() == DocumentSourceType.IMAGE ? "image-source" : "scanned-pdf");
        String ocrLang = resolveOcrLanguage();
        addDiagnostic(ir, "lang", ocrLang);

        String tesseractPath = resolveTesseractPath();
        if (tesseractPath == null) {
            addDiagnostic(ir, "fallback", "ocr-disabled");
            addDiagnostic(ir, "backend", "tesseract-not-found");
            return;
        }

        addDiagnostic(ir, "backend", "tesseract");
        addDiagnostic(ir, "strategy", profile.sourceType() == DocumentSourceType.IMAGE ? "image-direct" : "pdf-render-then-ocr");

        try {
            switch (profile.sourceType()) {
                case IMAGE -> extractImageOcr(source, ir, tesseractPath, ocrLang);
                case PDF -> extractScannedPdfOcr(source, ir, tesseractPath, ocrLang);
                default -> {
                    addDiagnostic(ir, "fallback", "unsupported-source-type");
                    addDiagnostic(ir, "skip", "unsupported-source-type");
                }
            }
        } catch (Exception e) {
            addDiagnostic(ir, "fallback", "ocr-error");
            addDiagnostic(ir, "error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private boolean requiresOcr(DocumentProfile profile) {
        return profile.sourceType() == DocumentSourceType.IMAGE
            || (profile.sourceType() == DocumentSourceType.PDF && profile.scanned());
    }

    private void extractImageOcr(Path source, DocumentIr ir, String tesseractPath, String ocrLang) throws IOException, InterruptedException {
        OcrAttempt attempt = runWithPreprocessing(source, tesseractPath, ocrLang, ir, "image");
        String text = attempt.text();
        if (text.isBlank()) {
            addDiagnostic(ir, "fallback", "empty-ocr-result");
            addDiagnostic(ir, "result", "empty-image-ocr");
            return;
        }

        double qualityScore = scoreTextQuality(text);
        addDiagnostic(ir, "qualityScore", qualityScore);
        addDiagnostic(ir, "qualityBand", qualityBand(qualityScore));
        if (qualityScore < 0.35d) {
            addDiagnostic(ir, "fallback", "low-quality-ocr-result");
        }

        String containerId = ensurePageContainer(ir, "Image 1", 0);
        addOcrNode(ir, containerId, text, "0");
        addDiagnostic(ir, "result", "image-ocr-extracted");
    }

    private void extractScannedPdfOcr(Path source, DocumentIr ir, String tesseractPath, String ocrLang) throws IOException, InterruptedException {
        try (PDDocument document = Loader.loadPDF(source.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            addDiagnostic(ir, "pageCount", document.getNumberOfPages());

            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                Path imageFile = Files.createTempFile("smartdoc-flow-ocr-page-", ".png");
                try {
                    var image = renderer.renderImageWithDPI(pageIndex, 200, ImageType.RGB);
                    javax.imageio.ImageIO.write(image, "png", imageFile.toFile());

                    OcrAttempt attempt = runWithPreprocessing(imageFile, tesseractPath, ocrLang, ir, "page-" + pageIndex);
                    String text = attempt.text();
                    if (text.isBlank()) {
                        addDiagnostic(ir, "fallback", "empty-page-ocr-result");
                        addDiagnostic(ir, "page-" + pageIndex, "empty-pdf-ocr");
                        addPageDiagnostic(ir, pageIndex, "result", "empty-pdf-ocr");
                        addPageDiagnostic(ir, pageIndex, "preprocess", attempt.mode());
                        continue;
                    }

                    double qualityScore = scoreTextQuality(text);
                    addDiagnostic(ir, "page-" + pageIndex + "-qualityScore", qualityScore);
                    addDiagnostic(ir, "page-" + pageIndex + "-qualityBand", qualityBand(qualityScore));
                    addPageDiagnostic(ir, pageIndex, "qualityScore", qualityScore);
                    addPageDiagnostic(ir, pageIndex, "qualityBand", qualityBand(qualityScore));
                    addPageDiagnostic(ir, pageIndex, "preprocess", attempt.mode());
                    addPageDiagnostic(ir, pageIndex, "result", "pdf-page-ocr-extracted");
                    if (qualityScore < 0.35d) {
                        addDiagnostic(ir, "fallback", "low-quality-page-ocr-result");
                        addPageDiagnostic(ir, pageIndex, "fallback", "low-quality-page-ocr-result");
                    }

                    String containerId = ensurePageContainer(ir, "Page " + (pageIndex + 1), pageIndex);
                    addOcrNode(ir, containerId, text, String.valueOf(pageIndex));
                } finally {
                    Files.deleteIfExists(imageFile);
                }
            }
        }
        addDiagnostic(ir, "result", "pdf-ocr-extracted");
    }

    private String ensurePageContainer(DocumentIr ir, String label, int index) {
        return ir.getContainers().stream()
            .filter(container -> container.index() == index)
            .findFirst()
            .map(Container::id)
            .orElseGet(() -> {
                String containerId = UUID.randomUUID().toString();
                ir.addContainer(new Container(
                    containerId,
                    ContainerType.PAGE,
                    index,
                    label,
                    0,
                    0,
                    Bbox.EMPTY,
                    Map.of()
                ));
                return containerId;
            });
    }

    private void addOcrNode(DocumentIr ir, String containerId, String text, String orderKey) {
        ir.addNode(new Node(
            UUID.randomUUID().toString(),
            NodeType.PARAGRAPH,
            containerId,
            Bbox.EMPTY,
            text,
            List.of(),
            0.7,
            orderKey,
            List.of(new SourceRef("ocrText", containerId, 0, text.length())),
            Map.of("source", "tesseract"),
            Set.of("OCR")
        ));
    }

    private void addDiagnostic(DocumentIr ir, String key, Object value) {
        ir.addDiagnostic(new Diagnostic("OCR", key, value, System.currentTimeMillis()));
    }

    private void addPageDiagnostic(DocumentIr ir, int pageIndex, String metric, Object value) {
        addDiagnostic(ir, "page." + pageIndex + "." + metric, value);
    }

    private String resolveTesseractPath() {
        String configured = System.getenv("SMARTDOC_FLOW_TESSERACT_PATH");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        Path defaultPath = Path.of("/opt/homebrew/bin/tesseract");
        if (Files.isExecutable(defaultPath)) {
            return defaultPath.toString();
        }
        return null;
    }

    private String resolveOcrLanguage() {
        String configured = System.getenv("SMARTDOC_FLOW_OCR_LANG");
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        return DEFAULT_OCR_LANG;
    }

    private String runTesseract(Path inputFile, String tesseractPath, String ocrLang) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(tesseractPath, inputFile.toString(), "stdout", "-l", ocrLang)
            .redirectErrorStream(true)
            .start();
        String output;
        try (var inputStream = process.getInputStream()) {
            output = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("tesseract exited with code " + exitCode);
        }
        return normalize(output);
    }

    private OcrAttempt runWithPreprocessing(Path inputFile, String tesseractPath, String ocrLang, DocumentIr ir, String scope) throws IOException, InterruptedException {
        String directText = runTesseract(inputFile, tesseractPath, ocrLang);
        if (!directText.isBlank()) {
            addDiagnostic(ir, scope + "-preprocess", "direct");
            return new OcrAttempt(directText, "direct");
        }

        Path enhancedFile = Files.createTempFile("smartdoc-flow-ocr-enhanced-", ".png");
        try {
            preprocessImage(inputFile, enhancedFile);
            String enhancedText = runTesseract(enhancedFile, tesseractPath, ocrLang);
            addDiagnostic(ir, scope + "-preprocess", enhancedText.isBlank() ? "enhanced-empty" : "enhanced-success");
            return new OcrAttempt(enhancedText, "enhanced");
        } finally {
            Files.deleteIfExists(enhancedFile);
        }
    }

    private void preprocessImage(Path inputFile, Path outputFile) throws IOException {
        BufferedImage sourceImage = javax.imageio.ImageIO.read(inputFile.toFile());
        if (sourceImage == null) {
            throw new IOException("unsupported image format");
        }
        BufferedImage scaled = scaleImage(sourceImage, 2.0d);
        BufferedImage binary = binarizeImage(scaled);
        javax.imageio.ImageIO.write(binary, "png", outputFile.toFile());
    }

    private BufferedImage scaleImage(BufferedImage image, double factor) {
        int width = Math.max(1, (int) Math.round(image.getWidth() * factor));
        int height = Math.max(1, (int) Math.round(image.getHeight() * factor));
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(image, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return scaled;
    }

    private BufferedImage binarizeImage(BufferedImage image) {
        BufferedImage binary = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = new Color(image.getRGB(x, y));
                int gray = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
                binary.setRGB(x, y, gray > 180 ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
            }
        }
        return binary;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").trim();
    }

    private double scoreTextQuality(String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return 0.0d;
        }
        long visibleChars = normalized.chars().filter(ch -> !Character.isWhitespace(ch)).count();
        long uniqueChars = normalized.chars().filter(ch -> !Character.isWhitespace(ch)).distinct().count();
        double lengthScore = Math.min(1.0d, visibleChars / 24.0d);
        double diversityScore = visibleChars == 0 ? 0.0d : Math.min(1.0d, uniqueChars / (double) visibleChars * 4.0d);
        return Math.round(((lengthScore * 0.6d) + (diversityScore * 0.4d)) * 100.0d) / 100.0d;
    }

    private String qualityBand(double qualityScore) {
        if (qualityScore >= 0.75d) {
            return "high";
        }
        if (qualityScore >= 0.35d) {
            return "medium";
        }
        return "low";
    }

    private record OcrAttempt(String text, String mode) {
    }
}
