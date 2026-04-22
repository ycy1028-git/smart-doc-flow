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
    @Override
    public void process(Path source, DocumentIr ir, DocumentProfile profile) {
        if (!requiresOcr(profile)) {
            return;
        }

        String tesseractPath = resolveTesseractPath();
        if (tesseractPath == null) {
            addDiagnostic(ir, "backend", "tesseract-not-found");
            return;
        }

        try {
            switch (profile.sourceType()) {
                case IMAGE -> extractImageOcr(source, ir, tesseractPath);
                case PDF -> extractScannedPdfOcr(source, ir, tesseractPath);
                default -> addDiagnostic(ir, "skip", "unsupported-source-type");
            }
        } catch (Exception e) {
            addDiagnostic(ir, "error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private boolean requiresOcr(DocumentProfile profile) {
        return profile.sourceType() == DocumentSourceType.IMAGE
            || (profile.sourceType() == DocumentSourceType.PDF && profile.scanned());
    }

    private void extractImageOcr(Path source, DocumentIr ir, String tesseractPath) throws IOException, InterruptedException {
        String text = runTesseract(source, tesseractPath);
        if (text.isBlank()) {
            addDiagnostic(ir, "result", "empty-image-ocr");
            return;
        }

        String containerId = ensurePageContainer(ir, "Image 1", 0);
        addOcrNode(ir, containerId, text, "0");
        addDiagnostic(ir, "result", "image-ocr-extracted");
    }

    private void extractScannedPdfOcr(Path source, DocumentIr ir, String tesseractPath) throws IOException, InterruptedException {
        try (PDDocument document = Loader.loadPDF(source.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);

            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                Path imageFile = Files.createTempFile("smartdoc-flow-ocr-page-", ".png");
                try {
                    var image = renderer.renderImageWithDPI(pageIndex, 200, ImageType.RGB);
                    javax.imageio.ImageIO.write(image, "png", imageFile.toFile());

                    String text = runTesseract(imageFile, tesseractPath);
                    if (text.isBlank()) {
                        addDiagnostic(ir, "page-" + pageIndex, "empty-pdf-ocr");
                        continue;
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

    private String runTesseract(Path inputFile, String tesseractPath) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(tesseractPath, inputFile.toString(), "stdout")
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

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").trim();
    }
}
