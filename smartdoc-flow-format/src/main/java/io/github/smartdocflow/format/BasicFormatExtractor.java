package io.github.smartdocflow.format;

import io.github.smartdocflow.common.model.DocumentSourceType;
import io.github.smartdocflow.core.model.DocumentProfile;
import io.github.smartdocflow.core.spi.FormatExtractionResult;
import io.github.smartdocflow.core.spi.FormatExtractor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public final class BasicFormatExtractor implements FormatExtractor {
    @Override
    public FormatExtractionResult extract(Path source, DocumentProfile profile) {
        if (profile.sourceType() == DocumentSourceType.PDF) {
            return extractPdf(source);
        }

        String placeholder = "SmartDoc-Flow skeleton extracted content for " + source.getFileName();
        return new FormatExtractionResult(source, placeholder, List.of(placeholder), List.of(List.of(placeholder)), 1);
    }

    private FormatExtractionResult extractPdf(Path source) {
        try (PDDocument document = Loader.loadPDF(source.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            List<String> pageTexts = new ArrayList<>();
            List<List<String>> pageLines = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = normalize(stripper.getText(document));
                pageTexts.add(text);
                pageLines.add(splitLines(text));
            }
            String fullText = String.join(System.lineSeparator() + System.lineSeparator(), pageTexts).trim();
            return new FormatExtractionResult(source, fullText, pageTexts, pageLines, document.getNumberOfPages());
        } catch (Exception e) {
            String placeholder = "Failed to extract PDF content from " + source.getFileName() + ": " + e.getMessage();
            return new FormatExtractionResult(source, placeholder, List.of(placeholder), List.of(splitLines(placeholder)), 1);
        }
    }

    private List<String> splitLines(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split("\\n", -1))
            .map(String::strip)
            .toList();
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").trim();
    }
}
