package io.github.smartdocflow.format;

import io.github.smartdocflow.common.model.DocumentSourceType;
import io.github.smartdocflow.core.model.DocumentProfile;
import io.github.smartdocflow.core.spi.DocumentProfiler;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public final class BasicDocumentProfiler implements DocumentProfiler {
    @Override
    public DocumentProfile profile(Path source) {
        String fileName = source.getFileName().toString().toLowerCase();
        DocumentSourceType sourceType = detectSourceType(fileName);
        if (sourceType == DocumentSourceType.PDF) {
            return profilePdf(source);
        }

        boolean image = sourceType == DocumentSourceType.IMAGE;
        return new DocumentProfile(sourceType, image, false, false, image);
    }

    private DocumentProfile profilePdf(Path source) {
        try (PDDocument document = Loader.loadPDF(source.toFile())) {
            PDFTextStripper textStripper = new PDFTextStripper();
            String text = textStripper.getText(document);
            boolean scanned = text == null || text.isBlank();
            return new DocumentProfile(DocumentSourceType.PDF, scanned, false, false, false);
        } catch (Exception ignored) {
            return new DocumentProfile(DocumentSourceType.PDF, false, false, false, false);
        }
    }

    private DocumentSourceType detectSourceType(String fileName) {
        if (fileName.endsWith(".pdf")) {
            return DocumentSourceType.PDF;
        }
        if (fileName.endsWith(".docx")) {
            return DocumentSourceType.DOCX;
        }
        if (fileName.endsWith(".xlsx")) {
            return DocumentSourceType.XLSX;
        }
        if (fileName.endsWith(".pptx")) {
            return DocumentSourceType.PPTX;
        }
        if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".tiff")) {
            return DocumentSourceType.IMAGE;
        }
        return DocumentSourceType.UNKNOWN;
    }
}
