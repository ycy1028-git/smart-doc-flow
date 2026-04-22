package io.ycy.smartdocflow.format;

import io.ycy.smartdocflow.common.model.DocumentSourceType;
import io.ycy.smartdocflow.common.model.Bbox;
import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.model.ir.Container;
import io.ycy.smartdocflow.core.model.ir.ContainerType;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.model.ir.DocumentMeta;
import io.ycy.smartdocflow.core.model.ir.Node;
import io.ycy.smartdocflow.core.model.ir.NodeType;
import io.ycy.smartdocflow.core.model.ir.SourceRef;
import io.ycy.smartdocflow.core.spi.FormatExtractionResult;
import io.ycy.smartdocflow.core.spi.FormatExtractor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

public final class BasicFormatExtractor implements FormatExtractor {
    @Override
    public FormatExtractionResult extract(Path source, DocumentProfile profile) {
        if (profile.sourceType() == DocumentSourceType.PDF) {
            return extractPdf(source);
        }
        if (profile.sourceType() == DocumentSourceType.PPTX) {
            return extractPptx(source);
        }

        String placeholder = "SmartDoc-Flow skeleton extracted content for " + source.getFileName();
        return new FormatExtractionResult(source, placeholder, List.of(placeholder), List.of(List.of(placeholder)), 1);
    }

    @Override
    public void extract(Path source, DocumentProfile profile, DocumentIr ir) {
        FormatExtractionResult result = extract(source, profile);
        updateMetaPageCount(ir, result.pageCount());

        String containerId = UUID.randomUUID().toString();
        ir.addContainer(new Container(
            containerId,
            defaultContainerType(profile.sourceType()),
            0,
            source.getFileName().toString(),
            0,
            0,
            Bbox.EMPTY,
            Map.of()
        ));

        if (!result.extractedText().isBlank()) {
            ir.addNode(new Node(
                UUID.randomUUID().toString(),
                NodeType.PARAGRAPH,
                containerId,
                Bbox.EMPTY,
                result.extractedText(),
                List.of(),
                profile.scanned() ? 0.80d : 0.95d,
                "0",
                List.of(new SourceRef("fallbackText", containerId, 0, result.extractedText().length())),
                Map.of(),
                Set.of("EXTRACT")
            ));
        }
    }

    private FormatExtractionResult extractPptx(Path source) {
        try (InputStream inputStream = java.nio.file.Files.newInputStream(source); XMLSlideShow slideShow = new XMLSlideShow(inputStream)) {
            List<String> pageTexts = new ArrayList<>();
            List<List<String>> pageLines = new ArrayList<>();

            int slideIndex = 1;
            for (XSLFSlide slide : slideShow.getSlides()) {
                String slideText = normalize(extractSlideText(slide, slideIndex));
                pageTexts.add(slideText);
                pageLines.add(splitLines(slideText));
                slideIndex++;
            }

            if (pageTexts.isEmpty()) {
                String placeholder = "No slides extracted from " + source.getFileName();
                return new FormatExtractionResult(source, placeholder, List.of(placeholder), List.of(splitLines(placeholder)), 1);
            }

            String fullText = String.join(System.lineSeparator() + System.lineSeparator(), pageTexts).trim();
            return new FormatExtractionResult(source, fullText, pageTexts, pageLines, pageTexts.size());
        } catch (Exception e) {
            String placeholder = "Failed to extract PPTX content from " + source.getFileName() + ": " + e.getMessage();
            return new FormatExtractionResult(source, placeholder, List.of(placeholder), List.of(splitLines(placeholder)), 1);
        }
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

    private String extractSlideText(XSLFSlide slide, int slideIndex) throws IOException {
        List<String> sections = new ArrayList<>();
        sections.add("Slide " + slideIndex);

        String title = normalize(slide.getTitle());
        if (!title.isBlank()) {
            sections.add(title);
        }

        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                String text = normalize(textShape.getText());
                if (!text.isBlank() && !text.equals(title)) {
                    sections.add(text);
                }
                continue;
            }
            if (shape instanceof XSLFTable table) {
                String tableText = normalize(extractTableText(table));
                if (!tableText.isBlank()) {
                    sections.add(tableText);
                }
                continue;
            }
            if (shape instanceof XSLFPictureShape pictureShape) {
                String pictureText = normalize(extractPictureText(pictureShape));
                if (!pictureText.isBlank()) {
                    sections.add(pictureText);
                }
            }
        }

        String notesText = normalize(extractNotesText(slide.getNotes()));
        if (!notesText.isBlank()) {
            sections.add("Notes");
            sections.add(notesText);
        }

        return String.join(System.lineSeparator(), sections).trim();
    }

    private String extractTableText(XSLFTable table) {
        List<String> rows = new ArrayList<>();
        for (XSLFTableRow row : table.getRows()) {
            List<String> cells = new ArrayList<>();
            for (XSLFTableCell cell : row.getCells()) {
                cells.add(normalize(cell.getText()));
            }
            rows.add(String.join(" | ", cells));
        }
        return String.join(System.lineSeparator(), rows);
    }

    private String extractPictureText(XSLFPictureShape pictureShape) {
        String name = pictureShape.getShapeName();
        if (name == null || name.isBlank()) {
            return "[Image]";
        }
        return "[Image: " + name.strip() + "]";
    }

    private String extractNotesText(XSLFNotes notes) {
        if (notes == null) {
            return "";
        }

        List<String> noteSections = new ArrayList<>();
        for (XSLFShape shape : notes.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                String text = normalize(textShape.getText());
                if (!text.isBlank()) {
                    noteSections.add(text);
                }
            }
        }
        return String.join(System.lineSeparator(), noteSections).trim();
    }

    private void updateMetaPageCount(DocumentIr ir, int pageCount) {
        var old = ir.getMeta();
        ir.setMeta(new DocumentMeta(
            old.documentId(),
            old.sourceType(),
            old.sourceName(),
            pageCount,
            old.languageHints(),
            old.scanned(),
            old.tableHeavy(),
            old.imageHeavy(),
            old.multiColumn(),
            old.generator(),
            old.parseVersion()
        ));
    }

    private ContainerType defaultContainerType(DocumentSourceType sourceType) {
        return switch (sourceType) {
            case PPTX -> ContainerType.SLIDE;
            case XLSX -> ContainerType.SHEET;
            case PDF, IMAGE, DOCX, UNKNOWN -> ContainerType.PAGE;
        };
    }
}
