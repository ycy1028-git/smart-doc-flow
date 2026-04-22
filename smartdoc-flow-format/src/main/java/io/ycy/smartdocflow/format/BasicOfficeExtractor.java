package io.ycy.smartdocflow.format;

import io.ycy.smartdocflow.common.model.Bbox;
import io.ycy.smartdocflow.common.model.DocumentSourceType;
import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.model.ir.Container;
import io.ycy.smartdocflow.core.model.ir.ContainerType;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
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
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

public final class BasicOfficeExtractor implements FormatExtractor {

    @Override
    public FormatExtractionResult extract(Path source, DocumentProfile profile) {
        if (profile.sourceType() == DocumentSourceType.PPTX) {
            return extractPptx(source);
        }

        String placeholder = "SmartDoc-Flow skeleton extracted content for " + source.getFileName();
        return new FormatExtractionResult(source, placeholder, List.of(placeholder), List.of(List.of(placeholder)), 1);
    }

    @Override
    public void extract(Path source, DocumentProfile profile, DocumentIr ir) {
        if (profile.sourceType() == DocumentSourceType.PPTX) {
            extractPptxIntoIr(source, ir);
        }
    }

    private void extractPptxIntoIr(Path source, DocumentIr ir) {
        try (InputStream inputStream = java.nio.file.Files.newInputStream(source);
             XMLSlideShow slideShow = new XMLSlideShow(inputStream)) {

            List<XSLFSlide> slides = slideShow.getSlides();
            updateMetaPageCount(ir, slides.size());

            int slideIndex = 0;
            for (XSLFSlide slide : slides) {
                String containerId = UUID.randomUUID().toString();
                ir.addContainer(new Container(
                    containerId,
                    ContainerType.SLIDE,
                    slideIndex,
                    "Slide " + (slideIndex + 1),
                    0, 0,
                    Bbox.EMPTY,
                    Map.of()
                ));

                String title = normalize(slide.getTitle());
                if (!title.isBlank()) {
                    ir.addNode(new Node(
                        UUID.randomUUID().toString(),
                        NodeType.TITLE,
                        containerId,
                        Bbox.EMPTY,
                        title,
                        List.of(),
                        1.0,
                        String.valueOf(slideIndex) + ".0",
                        List.of(new SourceRef("pptShape", containerId, 0, title.length())),
                        Map.of(),
                        Set.of("EXTRACT")
                    ));
                }

                int shapeOrder = 1;
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String text = normalize(textShape.getText());
                        if (!text.isBlank() && !text.equals(title)) {
                            ir.addNode(new Node(
                                UUID.randomUUID().toString(),
                                NodeType.PARAGRAPH,
                                containerId,
                                Bbox.EMPTY,
                                text,
                                List.of(),
                                1.0,
                                String.valueOf(slideIndex) + "." + shapeOrder,
                                List.of(new SourceRef("pptShape", containerId, 0, text.length())),
                                Map.of(),
                                Set.of("EXTRACT")
                            ));
                        }
                        shapeOrder++;
                        continue;
                    }
                    if (shape instanceof XSLFTable table) {
                        String tableText = normalize(extractTableText(table));
                        if (!tableText.isBlank()) {
                            ir.addNode(new Node(
                                UUID.randomUUID().toString(),
                                NodeType.TABLE,
                                containerId,
                                Bbox.EMPTY,
                                tableText,
                                List.of(),
                                1.0,
                                String.valueOf(slideIndex) + "." + shapeOrder,
                                List.of(new SourceRef("pptShape", containerId, 0, tableText.length())),
                                Map.of(),
                                Set.of("EXTRACT")
                            ));
                        }
                        shapeOrder++;
                        continue;
                    }
                    if (shape instanceof XSLFPictureShape pictureShape) {
                        String pictureText = normalize(extractPictureText(pictureShape));
                        if (!pictureText.isBlank()) {
                            ir.addNode(new Node(
                                UUID.randomUUID().toString(),
                                NodeType.FIGURE,
                                containerId,
                                Bbox.EMPTY,
                                pictureText,
                                List.of(),
                                1.0,
                                String.valueOf(slideIndex) + "." + shapeOrder,
                                List.of(new SourceRef("pptShape", containerId, 0, pictureText.length())),
                                Map.of(),
                                Set.of("EXTRACT")
                            ));
                        }
                        shapeOrder++;
                    }
                }

                String notesText = normalize(extractNotesText(slide.getNotes()));
                if (!notesText.isBlank()) {
                    String labeledNotes = "Notes\n" + notesText;
                    ir.addNode(new Node(
                        UUID.randomUUID().toString(),
                        NodeType.NOTE,
                        containerId,
                        Bbox.EMPTY,
                        labeledNotes,
                        List.of(),
                        1.0,
                        String.valueOf(slideIndex) + ".notes",
                        List.of(new SourceRef("pptShape", containerId, 0, labeledNotes.length())),
                        Map.of(),
                        Set.of("EXTRACT")
                    ));
                }

                slideIndex++;
            }
        } catch (Exception ignored) {
        }
    }

    private FormatExtractionResult extractPptx(Path source) {
        try (InputStream inputStream = java.nio.file.Files.newInputStream(source);
             XMLSlideShow slideShow = new XMLSlideShow(inputStream)) {
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

    private void updateMetaPageCount(DocumentIr ir, int pageCount) {
        var old = ir.getMeta();
        ir.setMeta(new io.ycy.smartdocflow.core.model.ir.DocumentMeta(
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
