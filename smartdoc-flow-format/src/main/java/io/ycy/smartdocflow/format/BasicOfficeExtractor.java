package io.ycy.smartdocflow.format;

import io.ycy.smartdocflow.common.model.Bbox;
import io.ycy.smartdocflow.common.model.DocumentSourceType;
import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.model.ir.Container;
import io.ycy.smartdocflow.core.model.ir.ContainerType;
import io.ycy.smartdocflow.core.model.ir.Asset;
import io.ycy.smartdocflow.core.model.ir.Diagnostic;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.model.ir.Node;
import io.ycy.smartdocflow.core.model.ir.NodeType;
import io.ycy.smartdocflow.core.model.ir.Relation;
import io.ycy.smartdocflow.core.model.ir.RelationType;
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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
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
    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    @Override
    public FormatExtractionResult extract(Path source, DocumentProfile profile) {
        if (profile.sourceType() == DocumentSourceType.DOCX) {
            return extractDocx(source);
        }
        if (profile.sourceType() == DocumentSourceType.XLSX) {
            return extractXlsx(source);
        }
        if (profile.sourceType() == DocumentSourceType.PPTX) {
            return extractPptx(source);
        }

        String placeholder = "SmartDoc-Flow skeleton extracted content for " + source.getFileName();
        return new FormatExtractionResult(source, placeholder, List.of(placeholder), List.of(List.of(placeholder)), 1);
    }

    @Override
    public void extract(Path source, DocumentProfile profile, DocumentIr ir) {
        if (profile.sourceType() == DocumentSourceType.DOCX) {
            ir.addDiagnostic(new Diagnostic("EXTRACT", "strategy", "docx-body-elements", System.currentTimeMillis()));
            extractDocxIntoIr(source, ir);
            return;
        }
        if (profile.sourceType() == DocumentSourceType.XLSX) {
            ir.addDiagnostic(new Diagnostic("EXTRACT", "strategy", "xlsx-sheet-rows", System.currentTimeMillis()));
            extractXlsxIntoIr(source, ir);
            return;
        }
        if (profile.sourceType() == DocumentSourceType.PPTX) {
            ir.addDiagnostic(new Diagnostic("EXTRACT", "strategy", "pptx-slide-shapes", System.currentTimeMillis()));
            extractPptxIntoIr(source, ir);
            return;
        }
        ir.addDiagnostic(new Diagnostic("EXTRACT", "reason", "unsupported-office-source", System.currentTimeMillis()));
    }

    private FormatExtractionResult extractDocx(Path source) {
        try (InputStream inputStream = java.nio.file.Files.newInputStream(source);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            String pageText = normalize(extractDocxBodyText(document));
            if (pageText.isBlank()) {
                String placeholder = "No DOCX content extracted from " + source.getFileName();
                return new FormatExtractionResult(source, placeholder, List.of(placeholder), List.of(splitLines(placeholder)), 1);
            }
            return new FormatExtractionResult(source, pageText, List.of(pageText), List.of(splitLines(pageText)), 1);
        } catch (Exception e) {
            String placeholder = "Failed to extract DOCX content from " + source.getFileName() + ": " + e.getMessage();
            return new FormatExtractionResult(source, placeholder, List.of(placeholder), List.of(splitLines(placeholder)), 1);
        }
    }

    private void extractDocxIntoIr(Path source, DocumentIr ir) {
        try (InputStream inputStream = java.nio.file.Files.newInputStream(source);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            updateMetaPageCount(ir, 1);
            int paragraphNodes = 0;
            int tableNodes = 0;

            String containerId = UUID.randomUUID().toString();
            Bbox pageBox = new Bbox(0, 0, 1000, 1400);
            ir.addContainer(new Container(
                containerId,
                ContainerType.PAGE,
                0,
                "Page 1",
                0, 0,
                pageBox,
                Map.of()
            ));

            attachDocxPictureAssets(ir, containerId, document);

            int order = 0;
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    String text = normalize(paragraph.getText());
                    if (text.isBlank()) {
                        continue;
                    }
                    NodeType nodeType = paragraph.getStyle() != null && paragraph.getStyle().toLowerCase().contains("title")
                        ? NodeType.TITLE
                        : NodeType.PARAGRAPH;
                    String nodeId = UUID.randomUUID().toString();
                    ir.addNode(new Node(
                        nodeId,
                        nodeType,
                        containerId,
                        estimateBlockBbox(order),
                        text,
                        List.of(),
                        1.0,
                        String.valueOf(order++),
                        List.of(new SourceRef("docxBody", containerId, 0, text.length())),
                        Map.of(
                            "nodeRole", nodeType == NodeType.TITLE ? "docx-title" : "docx-paragraph",
                            "pageIndex", 0
                        ),
                        Set.of("EXTRACT")
                    ));
                    ir.addRelation(new io.ycy.smartdocflow.core.model.ir.Relation(containerId, nodeId, io.ycy.smartdocflow.core.model.ir.RelationType.CHILD_OF, 1.0, Map.of("stage", "EXTRACT")));
                    paragraphNodes++;
                    continue;
                }
                if (element instanceof XWPFTable table) {
                    String tableText = normalize(extractDocxTableText(table));
                    if (tableText.isBlank()) {
                        continue;
                    }
                    String nodeId = UUID.randomUUID().toString();
                    ir.addNode(new Node(
                        nodeId,
                        NodeType.TABLE,
                        containerId,
                        estimateBlockBbox(order),
                        tableText,
                        List.of(),
                        1.0,
                        String.valueOf(order++),
                        List.of(new SourceRef("docxTable", containerId, 0, tableText.length())),
                        Map.of(
                            "nodeRole", "docx-table",
                            "pageIndex", 0,
                            "rowCount", table.getRows().size()
                        ),
                        Set.of("EXTRACT")
                    ));
                    ir.addRelation(new io.ycy.smartdocflow.core.model.ir.Relation(containerId, nodeId, io.ycy.smartdocflow.core.model.ir.RelationType.CHILD_OF, 1.0, Map.of("stage", "EXTRACT")));
                    tableNodes++;
                }
            }
            ir.addDiagnostic(new Diagnostic("EXTRACT", "paragraphNodes", paragraphNodes, System.currentTimeMillis()));
            ir.addDiagnostic(new Diagnostic("EXTRACT", "tableNodes", tableNodes, System.currentTimeMillis()));
            ir.addDiagnostic(new Diagnostic("EXTRACT", "assetCount", ir.getAssets().size(), System.currentTimeMillis()));
            ir.addDiagnostic(new Diagnostic("EXTRACT", "reason", paragraphNodes == 0 && tableNodes == 0 ? "no-docx-content-extracted" : "docx-content-extracted", System.currentTimeMillis()));
        } catch (Exception e) {
            ir.addDiagnostic(new Diagnostic("EXTRACT", "fallback", "docx-extract-failed", System.currentTimeMillis()));
            ir.addDiagnostic(new Diagnostic("EXTRACT", "error", e.getClass().getSimpleName(), System.currentTimeMillis()));
        }
    }

    private FormatExtractionResult extractXlsx(Path source) {
        try (InputStream inputStream = java.nio.file.Files.newInputStream(source);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            List<String> pageTexts = new ArrayList<>();
            List<List<String>> pageLines = new ArrayList<>();

            for (Sheet sheet : workbook) {
                String sheetText = normalize(extractSheetText(sheet));
                if (sheetText.isBlank()) {
                    continue;
                }
                pageTexts.add(sheetText);
                pageLines.add(splitLines(sheetText));
            }

            if (pageTexts.isEmpty()) {
                String placeholder = "No XLSX content extracted from " + source.getFileName();
                return new FormatExtractionResult(source, placeholder, List.of(placeholder), List.of(splitLines(placeholder)), 1);
            }

            String fullText = String.join(System.lineSeparator() + System.lineSeparator(), pageTexts).trim();
            return new FormatExtractionResult(source, fullText, pageTexts, pageLines, pageTexts.size());
        } catch (Exception e) {
            String placeholder = "Failed to extract XLSX content from " + source.getFileName() + ": " + e.getMessage();
            return new FormatExtractionResult(source, placeholder, List.of(placeholder), List.of(splitLines(placeholder)), 1);
        }
    }

    private void extractXlsxIntoIr(Path source, DocumentIr ir) {
        try (InputStream inputStream = java.nio.file.Files.newInputStream(source);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            int sheetCount = workbook.getNumberOfSheets();
            updateMetaPageCount(ir, Math.max(sheetCount, 1));
            int extractedSheets = 0;

            int sheetIndex = 0;
            for (Sheet sheet : workbook) {
                String sheetText = normalize(extractSheetText(sheet));
                if (sheetText.isBlank()) {
                    sheetIndex++;
                    continue;
                }

                String containerId = UUID.randomUUID().toString();
                Bbox sheetBox = new Bbox(0, 0, 1200, 1600);
                ir.addContainer(new Container(
                    containerId,
                    ContainerType.SHEET,
                    sheetIndex,
                    sheet.getSheetName(),
                    0, 0,
                    sheetBox,
                    Map.of()
                ));

                String labeledTable = sheet.getSheetName() + System.lineSeparator() + sheetText;
                String nodeId = UUID.randomUUID().toString();
                ir.addNode(new Node(
                    nodeId,
                    NodeType.TABLE,
                    containerId,
                    estimateTableBbox(sheetIndex, 0),
                    labeledTable,
                    List.of(),
                    1.0,
                    String.valueOf(sheetIndex),
                    List.of(new SourceRef("xlsxSheet", containerId, 0, labeledTable.length())),
                    Map.of(
                        "nodeRole", "xlsx-sheet-table",
                        "sheetName", sheet.getSheetName(),
                        "sheetIndex", sheetIndex
                    ),
                    Set.of("EXTRACT")
                ));
                ir.addRelation(new io.ycy.smartdocflow.core.model.ir.Relation(containerId, nodeId, io.ycy.smartdocflow.core.model.ir.RelationType.CHILD_OF, 1.0, Map.of("stage", "EXTRACT")));
                extractedSheets++;
                sheetIndex++;
            }
            ir.addDiagnostic(new Diagnostic("EXTRACT", "sheetCount", sheetCount, System.currentTimeMillis()));
            ir.addDiagnostic(new Diagnostic("EXTRACT", "extractedSheets", extractedSheets, System.currentTimeMillis()));
            ir.addDiagnostic(new Diagnostic("EXTRACT", "reason", extractedSheets == 0 ? "no-xlsx-content-extracted" : "xlsx-content-extracted", System.currentTimeMillis()));
        } catch (Exception e) {
            ir.addDiagnostic(new Diagnostic("EXTRACT", "fallback", "xlsx-extract-failed", System.currentTimeMillis()));
            ir.addDiagnostic(new Diagnostic("EXTRACT", "error", e.getClass().getSimpleName(), System.currentTimeMillis()));
        }
    }

    private void extractPptxIntoIr(Path source, DocumentIr ir) {
        try (InputStream inputStream = java.nio.file.Files.newInputStream(source);
             XMLSlideShow slideShow = new XMLSlideShow(inputStream)) {

            List<XSLFSlide> slides = slideShow.getSlides();
            updateMetaPageCount(ir, slides.size());
            int extractedSlides = 0;

            int slideIndex = 0;
            for (XSLFSlide slide : slides) {
                String containerId = UUID.randomUUID().toString();
                Bbox slideBox = new Bbox(0, 0, 1280, 720);
                ir.addContainer(new Container(
                    containerId,
                    ContainerType.SLIDE,
                    slideIndex,
                    "Slide " + (slideIndex + 1),
                    0, 0,
                    slideBox,
                    Map.of()
                ));

                String title = normalize(slide.getTitle());
                if (!title.isBlank()) {
                    String nodeId = UUID.randomUUID().toString();
                    ir.addNode(new Node(
                        nodeId,
                        NodeType.TITLE,
                        containerId,
                        estimateSlideBbox(0),
                        title,
                        List.of(),
                        1.0,
                        String.valueOf(slideIndex) + ".0",
                        List.of(new SourceRef("pptShape", containerId, 0, title.length())),
                        Map.of(
                            "nodeRole", "ppt-title",
                            "slideIndex", slideIndex,
                            "slideLabel", "Slide " + (slideIndex + 1)
                        ),
                        Set.of("EXTRACT")
                    ));
                    ir.addRelation(new io.ycy.smartdocflow.core.model.ir.Relation(containerId, nodeId, io.ycy.smartdocflow.core.model.ir.RelationType.CHILD_OF, 1.0, Map.of("stage", "EXTRACT")));
                }

                int shapeOrder = 1;
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String text = normalize(textShape.getText());
                        if (!text.isBlank() && !text.equals(title)) {
                            String nodeId = UUID.randomUUID().toString();
                            ir.addNode(new Node(
                                nodeId,
                                NodeType.PARAGRAPH,
                                containerId,
                                estimateSlideBbox(shapeOrder),
                                text,
                                List.of(),
                                1.0,
                                String.valueOf(slideIndex) + "." + shapeOrder,
                                List.of(new SourceRef("pptShape", containerId, 0, text.length())),
                                Map.of(
                                    "nodeRole", "ppt-text",
                                    "slideIndex", slideIndex,
                                    "shapeOrder", shapeOrder
                                ),
                                Set.of("EXTRACT")
                            ));
                            ir.addRelation(new io.ycy.smartdocflow.core.model.ir.Relation(containerId, nodeId, io.ycy.smartdocflow.core.model.ir.RelationType.CHILD_OF, 1.0, Map.of("stage", "EXTRACT")));
                        }
                        shapeOrder++;
                        continue;
                    }
                    if (shape instanceof XSLFTable table) {
                        String tableText = normalize(extractTableText(table));
                        if (!tableText.isBlank()) {
                            String nodeId = UUID.randomUUID().toString();
                            ir.addNode(new Node(
                                nodeId,
                                NodeType.TABLE,
                                containerId,
                                estimateTableBbox(slideIndex, shapeOrder),
                                tableText,
                                List.of(),
                                1.0,
                                String.valueOf(slideIndex) + "." + shapeOrder,
                                List.of(new SourceRef("pptShape", containerId, 0, tableText.length())),
                                Map.of(
                                    "nodeRole", "ppt-table",
                                    "slideIndex", slideIndex,
                                    "shapeOrder", shapeOrder,
                                    "rowCount", table.getRows().size()
                                ),
                                Set.of("EXTRACT")
                            ));
                            ir.addRelation(new io.ycy.smartdocflow.core.model.ir.Relation(containerId, nodeId, io.ycy.smartdocflow.core.model.ir.RelationType.CHILD_OF, 1.0, Map.of("stage", "EXTRACT")));
                        }
                        shapeOrder++;
                        continue;
                    }
                    if (shape instanceof XSLFPictureShape pictureShape) {
                        String pictureText = normalize(extractPictureText(pictureShape));
                        if (!pictureText.isBlank()) {
                            String nodeId = UUID.randomUUID().toString();
                            Bbox pictureBbox = estimateSlideBbox(shapeOrder);
                            ir.addNode(new Node(
                                nodeId,
                                NodeType.FIGURE,
                                containerId,
                                pictureBbox,
                                pictureText,
                                List.of(),
                                1.0,
                                String.valueOf(slideIndex) + "." + shapeOrder,
                                List.of(new SourceRef("pptShape", containerId, 0, pictureText.length())),
                                Map.of(
                                    "nodeRole", "ppt-image",
                                    "slideIndex", slideIndex,
                                    "shapeOrder", shapeOrder,
                                    "shapeName", pictureShape.getShapeName()
                                ),
                                Set.of("EXTRACT")
                            ));
                            ir.addRelation(new Relation(containerId, nodeId, RelationType.CHILD_OF, 1.0, Map.of("stage", "EXTRACT")));
                            attachPictureAsset(ir, containerId, nodeId, pictureShape, pictureBbox, slideIndex, shapeOrder);
                        }
                        shapeOrder++;
                    }
                }

                String notesText = normalize(extractNotesText(slide.getNotes()));
                if (!notesText.isBlank()) {
                    String labeledNotes = "Notes\n" + notesText;
                    String nodeId = UUID.randomUUID().toString();
                    ir.addNode(new Node(
                        nodeId,
                        NodeType.NOTE,
                        containerId,
                        estimateSlideNotesBbox(),
                        labeledNotes,
                        List.of(),
                        1.0,
                        String.valueOf(slideIndex) + ".notes",
                        List.of(new SourceRef("pptShape", containerId, 0, labeledNotes.length())),
                        Map.of(
                            "nodeRole", "ppt-notes",
                            "slideIndex", slideIndex
                        ),
                        Set.of("EXTRACT")
                    ));
                    ir.addRelation(new io.ycy.smartdocflow.core.model.ir.Relation(containerId, nodeId, io.ycy.smartdocflow.core.model.ir.RelationType.CHILD_OF, 1.0, Map.of("stage", "EXTRACT")));
                }

                extractedSlides++;
                slideIndex++;
            }
            ir.addDiagnostic(new Diagnostic("EXTRACT", "slideCount", slides.size(), System.currentTimeMillis()));
            ir.addDiagnostic(new Diagnostic("EXTRACT", "extractedSlides", extractedSlides, System.currentTimeMillis()));
            ir.addDiagnostic(new Diagnostic("EXTRACT", "reason", extractedSlides == 0 ? "no-pptx-content-extracted" : "pptx-content-extracted", System.currentTimeMillis()));
        } catch (Exception e) {
            ir.addDiagnostic(new Diagnostic("EXTRACT", "fallback", "pptx-extract-failed", System.currentTimeMillis()));
            ir.addDiagnostic(new Diagnostic("EXTRACT", "error", e.getClass().getSimpleName(), System.currentTimeMillis()));
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

    private void attachPictureAsset(DocumentIr ir, String containerId, String nodeId, XSLFPictureShape pictureShape, Bbox bbox, int slideIndex, int shapeOrder) {
        try {
            var pictureData = pictureShape.getPictureData();
            String assetId = UUID.randomUUID().toString();
            String fileName = pictureData == null ? null : pictureData.getFileName();
            String mimeType = pictureData == null ? null : pictureData.getContentType();
            byte[] data = pictureData == null ? null : pictureData.getData();
            String assetName = fileName == null || fileName.isBlank() ? pictureShape.getShapeName() : fileName;
            ir.addAsset(new Asset(
                assetId,
                "image",
                containerId,
                assetName,
                mimeType == null ? "application/octet-stream" : mimeType,
                data == null ? 0L : data.length,
                assetName,
                bbox,
                Map.of(
                    "source", "pptx-picture-shape",
                    "slideIndex", slideIndex,
                    "shapeOrder", shapeOrder,
                    "shapeName", pictureShape.getShapeName(),
                    "hasBinary", pictureData != null
                )
            ));
            ir.addRelation(new Relation(nodeId, assetId, RelationType.REFERS_TO, 1.0, Map.of("stage", "EXTRACT", "kind", "figure-asset")));
        } catch (Exception ignored) {
            // Asset attachment is best-effort and must not break basic text extraction.
        }
    }

    private void attachDocxPictureAssets(DocumentIr ir, String containerId, XWPFDocument document) {
        int pictureIndex = 0;
        for (XWPFPictureData pictureData : document.getAllPictures()) {
            String assetId = UUID.randomUUID().toString();
            String fileName = pictureData.getFileName();
            String assetName = fileName == null || fileName.isBlank() ? "docx-image-" + pictureIndex : fileName;
            byte[] data = pictureData.getData();
            ir.addAsset(new Asset(
                assetId,
                "image",
                containerId,
                assetName,
                pictureData.getPackagePart() == null ? "application/octet-stream" : pictureData.getPackagePart().getContentType(),
                data == null ? 0L : data.length,
                assetName,
                estimateBlockBbox(pictureIndex),
                Map.of(
                    "source", "docx-picture-data",
                    "pageIndex", 0,
                    "pictureIndex", pictureIndex,
                    "hasBinary", data != null
                )
            ));
            pictureIndex++;
        }
    }

    private String extractDocxBodyText(XWPFDocument document) {
        List<String> sections = new ArrayList<>();
        for (IBodyElement element : document.getBodyElements()) {
            if (element instanceof XWPFParagraph paragraph) {
                String text = normalize(paragraph.getText());
                if (!text.isBlank()) {
                    sections.add(text);
                }
                continue;
            }
            if (element instanceof XWPFTable table) {
                String tableText = normalize(extractDocxTableText(table));
                if (!tableText.isBlank()) {
                    sections.add(tableText);
                }
            }
        }
        return String.join(System.lineSeparator() + System.lineSeparator(), sections).trim();
    }

    private String extractDocxTableText(XWPFTable table) {
        List<String> rows = new ArrayList<>();
        for (XWPFTableRow row : table.getRows()) {
            List<String> cells = new ArrayList<>();
            for (XWPFTableCell cell : row.getTableCells()) {
                cells.add(normalize(cell.getText()));
            }
            rows.add(String.join(" | ", cells));
        }
        return String.join(System.lineSeparator(), rows);
    }

    private String extractSheetText(Sheet sheet) {
        List<String> rows = new ArrayList<>();
        for (Row row : sheet) {
            List<String> cells = new ArrayList<>();
            short firstCell = row.getFirstCellNum();
            short lastCell = row.getLastCellNum();
            if (firstCell < 0 || lastCell < 0) {
                continue;
            }
            for (int cellIndex = firstCell; cellIndex < lastCell; cellIndex++) {
                Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                cells.add(cell == null ? "" : normalize(DATA_FORMATTER.formatCellValue(cell)));
            }
            String rowText = String.join(" | ", cells).stripTrailing();
            if (!rowText.isBlank()) {
                rows.add(rowText);
            }
        }
        return String.join(System.lineSeparator(), rows);
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

    private Bbox estimateBlockBbox(int orderIndex) {
        double y = 80 + (orderIndex * 110);
        return new Bbox(80, y, 840, 72);
    }

    private Bbox estimateTableBbox(int containerIndex, int orderIndex) {
        double y = 140 + (orderIndex * 140);
        return new Bbox(60, y, 900, 180);
    }

    private Bbox estimateSlideBbox(int orderIndex) {
        double y = 60 + (orderIndex * 90);
        return new Bbox(60, y, 900, 72);
    }

    private Bbox estimateSlideNotesBbox() {
        return new Bbox(60, 560, 900, 120);
    }
}
