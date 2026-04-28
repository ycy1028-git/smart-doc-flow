package io.ycy.smartdocflow.ocr;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.ycy.smartdocflow.common.model.DocumentSourceType;
import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.model.ir.DocumentMeta;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class BasicOcrProviderTest {
    private final BasicOcrProvider provider = new BasicOcrProvider();

    @Test
    void skipsNonScannedPdf() {
        DocumentIr ir = createIr(DocumentSourceType.PDF, "plain.pdf", false);

        provider.process(Path.of("plain.pdf"), ir, new DocumentProfile(DocumentSourceType.PDF, false, false, false, false));

        assertTrue(ir.getNodes().isEmpty());
        assertTrue(ir.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.key().equals("decision") && "skip-ocr".equals(String.valueOf(diagnostic.value()))));
        assertTrue(ir.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.key().equals("reason") && "ocr-not-required".equals(String.valueOf(diagnostic.value()))));
    }

    @Test
    void recordsDiagnosticWhenTesseractIsUnavailableForImage() throws IOException {
        Path imageFile = Files.createTempFile("smartdoc-flow-ocr-", ".png");
        try {
            DocumentIr ir = createIr(DocumentSourceType.IMAGE, imageFile.getFileName().toString(), false);

            provider.process(imageFile, ir, new DocumentProfile(DocumentSourceType.IMAGE, true, false, false, true));

            assertTrue(ir.getNodes().isEmpty());
            assertTrue(ir.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("decision") && "run-ocr".equals(String.valueOf(diagnostic.value()))));
            assertTrue(ir.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("reason") && "image-source".equals(String.valueOf(diagnostic.value()))));
            assertTrue(ir.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("fallback") && "ocr-disabled".equals(String.valueOf(diagnostic.value()))));
            assertTrue(ir.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("backend") && "tesseract-not-found".equals(String.valueOf(diagnostic.value()))));
            assertTrue(ir.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("lang") && "chi_sim+eng".equals(String.valueOf(diagnostic.value()))));
        } finally {
            Files.deleteIfExists(imageFile);
        }
    }

    private DocumentIr createIr(DocumentSourceType sourceType, String sourceName, boolean scanned) {
        return new DocumentIr(new DocumentMeta(
            "test-doc",
            sourceType,
            sourceName,
            0,
            List.of(),
            scanned,
            false,
            false,
            false,
            "test",
            "test"
        ));
    }

    @Test
    void compositeProviderRecordsRouteSelection() throws IOException {
        Path imageFile = Files.createTempFile("smartdoc-flow-ocr-route-", ".png");
        try {
            DocumentIr ir = createIr(DocumentSourceType.IMAGE, imageFile.getFileName().toString(), false);

            CompositeOcrProvider.basic().process(imageFile, ir, new DocumentProfile(DocumentSourceType.IMAGE, true, false, false, false));

            assertTrue(ir.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("route") && "image-basic".equals(String.valueOf(diagnostic.value()))));
            assertTrue(ir.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("provider") && ("auto".equals(String.valueOf(diagnostic.value())) || "tesseract".equals(String.valueOf(diagnostic.value())))));
        } finally {
            Files.deleteIfExists(imageFile);
        }
    }

    @Test
    void compositeProviderRecordsHighDetailRouteSelection() throws IOException {
        Path imageFile = Files.createTempFile("smartdoc-flow-ocr-route-hi-", ".png");
        try {
            DocumentIr ir = createIr(DocumentSourceType.IMAGE, imageFile.getFileName().toString(), false);

            CompositeOcrProvider.basic().process(imageFile, ir, new DocumentProfile(DocumentSourceType.IMAGE, true, false, false, true));

            assertTrue(ir.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("route") && "image-high-detail".equals(String.valueOf(diagnostic.value()))));
            assertTrue(ir.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("costTier") && "high".equals(String.valueOf(diagnostic.value()))));
            assertTrue(ir.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("qualityHint") && "detail-priority".equals(String.valueOf(diagnostic.value()))));
        } finally {
            Files.deleteIfExists(imageFile);
        }
    }

    @Test
    void compositeProviderRecordsScannedPdfRouteSelection() {
        DocumentIr ir = createIr(DocumentSourceType.PDF, "scan.pdf", true);

        CompositeOcrProvider.basic("noop").process(Path.of("scan.pdf"), ir, new DocumentProfile(DocumentSourceType.PDF, true, false, false, false));

        assertTrue(ir.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("route") && "scanned-pdf-basic".equals(String.valueOf(diagnostic.value()))));
        assertTrue(ir.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("provider") && "noop".equals(String.valueOf(diagnostic.value()))));
    }

    @Test
    void compositeProviderSupportsNoopBackendSelection() throws IOException {
        Path imageFile = Files.createTempFile("smartdoc-flow-ocr-route-noop-", ".png");
        try {
            DocumentIr ir = createIr(DocumentSourceType.IMAGE, imageFile.getFileName().toString(), false);

            CompositeOcrProvider.basic("noop").process(imageFile, ir, new DocumentProfile(DocumentSourceType.IMAGE, true, false, false, true));

            assertTrue(ir.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("provider") && "noop".equals(String.valueOf(diagnostic.value()))));
            assertTrue(ir.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("backend") && "noop".equals(String.valueOf(diagnostic.value()))));
            assertTrue(ir.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("fallback") && "provider-disabled".equals(String.valueOf(diagnostic.value()))));
        } finally {
            Files.deleteIfExists(imageFile);
        }
    }

    @Test
    void scannedPdfNoopRouteKeepsPageLevelDiagnosticsShapeStable() {
        DocumentIr ir = createIr(DocumentSourceType.PDF, "scan.pdf", true);

        CompositeOcrProvider.basic("noop").process(Path.of("scan.pdf"), ir, new DocumentProfile(DocumentSourceType.PDF, true, false, false, false));

        assertTrue(ir.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("route") && "scanned-pdf-basic".equals(String.valueOf(diagnostic.value()))));
        assertTrue(ir.getDiagnostics().stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("backend") && "noop".equals(String.valueOf(diagnostic.value()))));
    }

    @Test
    void qualityScoreHeuristicDistinguishesLowAndHighSignals() throws Exception {
        Method scoreMethod = BasicOcrProvider.class.getDeclaredMethod("scoreTextQuality", String.class);
        scoreMethod.setAccessible(true);
        Method bandMethod = BasicOcrProvider.class.getDeclaredMethod("qualityBand", double.class);
        bandMethod.setAccessible(true);

        double lowScore = (double) scoreMethod.invoke(provider, "1");
        double highScore = (double) scoreMethod.invoke(provider, "SmartDoc Flow OCR baseline result");
        String lowBand = String.valueOf(bandMethod.invoke(provider, lowScore));
        String highBand = String.valueOf(bandMethod.invoke(provider, highScore));

        assertTrue(lowScore < highScore);
        assertTrue("low".equals(lowBand) || "medium".equals(lowBand));
        assertTrue("high".equals(highBand) || "medium".equals(highBand));
    }

    @Test
    void preprocessImageCreatesUsableBinaryOutput() throws Exception {
        Method preprocessMethod = BasicOcrProvider.class.getDeclaredMethod("preprocessImage", Path.class, Path.class);
        preprocessMethod.setAccessible(true);

        Path input = Files.createTempFile("smartdoc-flow-ocr-pre-", ".png");
        Path output = Files.createTempFile("smartdoc-flow-ocr-pre-out-", ".png");
        try {
            BufferedImage image = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    image.setRGB(x, y, (x + y) % 2 == 0 ? Color.WHITE.getRGB() : Color.GRAY.getRGB());
                }
            }
            javax.imageio.ImageIO.write(image, "png", input.toFile());

            preprocessMethod.invoke(provider, input, output);

            BufferedImage processed = javax.imageio.ImageIO.read(output.toFile());
            assertTrue(processed.getWidth() >= image.getWidth() * 2 - 1);
            assertTrue(processed.getHeight() >= image.getHeight() * 2 - 1);
        } finally {
            Files.deleteIfExists(input);
            Files.deleteIfExists(output);
        }
    }
}
