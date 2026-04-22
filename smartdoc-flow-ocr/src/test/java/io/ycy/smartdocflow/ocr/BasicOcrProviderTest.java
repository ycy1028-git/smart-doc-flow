package io.ycy.smartdocflow.ocr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.ycy.smartdocflow.common.model.DocumentSourceType;
import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.model.ir.DocumentMeta;
import java.io.IOException;
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
        assertTrue(ir.getDiagnostics().isEmpty());
    }

    @Test
    void recordsDiagnosticWhenTesseractIsUnavailableForImage() throws IOException {
        Path imageFile = Files.createTempFile("smartdoc-flow-ocr-", ".png");
        try {
            DocumentIr ir = createIr(DocumentSourceType.IMAGE, imageFile.getFileName().toString(), false);

            provider.process(imageFile, ir, new DocumentProfile(DocumentSourceType.IMAGE, true, false, false, true));

            assertTrue(ir.getNodes().isEmpty());
            assertEquals(1, ir.getDiagnostics().size());
            assertEquals("OCR", ir.getDiagnostics().getFirst().stage());
            assertEquals("backend", ir.getDiagnostics().getFirst().key());
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
}
