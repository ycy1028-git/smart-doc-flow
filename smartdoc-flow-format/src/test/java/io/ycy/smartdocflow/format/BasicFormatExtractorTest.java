package io.ycy.smartdocflow.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.ycy.smartdocflow.common.model.DocumentSourceType;
import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.model.ir.DocumentMeta;
import io.ycy.smartdocflow.core.model.ir.NodeType;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class BasicFormatExtractorTest {
    private final BasicFormatExtractor extractor = new BasicFormatExtractor();
    private final BasicOfficeExtractor officeExtractor = new BasicOfficeExtractor();

    @Test
    void extractsBasicDocxContent() throws IOException {
        Path docxFile = createSampleDocx();
        try {
            DocumentProfile profile = new DocumentProfile(DocumentSourceType.DOCX, false, false, false, false);

            var result = officeExtractor.extract(docxFile, profile);

            assertEquals(1, result.pageCount());
            assertEquals(1, result.pageTexts().size());
            assertTrue(result.extractedText().contains("Project Overview"));
            assertTrue(result.extractedText().contains("This document summarizes the current implementation status."));
            assertTrue(result.extractedText().contains("Metric | Value"));
            assertTrue(result.extractedText().contains("Coverage | Baseline"));
        } finally {
            Files.deleteIfExists(docxFile);
        }
    }

    @Test
    void extractsBasicXlsxContent() throws IOException {
        Path xlsxFile = createSampleXlsx();
        try {
            DocumentProfile profile = new DocumentProfile(DocumentSourceType.XLSX, false, false, false, false);

            var result = officeExtractor.extract(xlsxFile, profile);

            assertEquals(1, result.pageCount());
            assertEquals(1, result.pageTexts().size());
            assertTrue(result.extractedText().contains("Metric | Value"));
            assertTrue(result.extractedText().contains("Coverage | Baseline"));
            assertTrue(result.extractedText().contains("Owner | Core Team"));
        } finally {
            Files.deleteIfExists(xlsxFile);
        }
    }

    @Test
    void extractsDocxIntoIrStructure() throws IOException {
        Path docxFile = createSampleDocx();
        try {
            DocumentProfile profile = new DocumentProfile(DocumentSourceType.DOCX, false, false, false, false);
            DocumentIr ir = createIr(DocumentSourceType.DOCX, docxFile);

            officeExtractor.extract(docxFile, profile, ir);

            assertEquals(1, ir.getMeta().pageCount());
            assertEquals(1, ir.getContainers().size());
            assertEquals(3, ir.getNodes().size());
            assertEquals(NodeType.TITLE, ir.getNodes().get(0).nodeType());
            assertEquals(NodeType.PARAGRAPH, ir.getNodes().get(1).nodeType());
            assertTrue(ir.getNodes().stream().anyMatch(node -> node.nodeType() == NodeType.TABLE && node.text().contains("Metric | Value")));
        } finally {
            Files.deleteIfExists(docxFile);
        }
    }

    @Test
    void extractsXlsxIntoIrStructure() throws IOException {
        Path xlsxFile = createSampleXlsx();
        try {
            DocumentProfile profile = new DocumentProfile(DocumentSourceType.XLSX, false, false, false, false);
            DocumentIr ir = createIr(DocumentSourceType.XLSX, xlsxFile);

            officeExtractor.extract(xlsxFile, profile, ir);

            assertEquals(1, ir.getMeta().pageCount());
            assertEquals(1, ir.getContainers().size());
            assertEquals(1, ir.getNodes().size());
            assertEquals(NodeType.TABLE, ir.getNodes().getFirst().nodeType());
            assertTrue(ir.getNodes().getFirst().text().contains("Summary"));
            assertTrue(ir.getNodes().getFirst().text().contains("Owner | Core Team"));
        } finally {
            Files.deleteIfExists(xlsxFile);
        }
    }

    @Test
    void extractsBasicPptxSlideContent() throws IOException {
        Path pptxFile = createSamplePptx();
        try {
            DocumentProfile profile = new DocumentProfile(DocumentSourceType.PPTX, false, false, false, false);

            var result = extractor.extract(pptxFile, profile);

            assertEquals(1, result.pageCount());
            assertEquals(1, result.pageTexts().size());
            assertTrue(result.extractedText().contains("Slide 1"));
            assertTrue(result.extractedText().contains("Quarterly Review"));
            assertTrue(result.extractedText().contains("Revenue grew by 20%"));
            assertTrue(result.pageLines().getFirst().contains("Quarterly Review"));
        } finally {
            Files.deleteIfExists(pptxFile);
        }
    }

    @Test
    void imageExtractionDoesNotReturnSkeletonPlaceholder() throws IOException {
        Path imageFile = createSampleImage();
        try {
            DocumentProfile profile = new DocumentProfile(DocumentSourceType.IMAGE, true, false, false, true);

            var result = extractor.extract(imageFile, profile);

            assertEquals(1, result.pageCount());
            assertTrue(result.extractedText().isBlank());
            assertTrue(result.pageTexts().isEmpty());
        } finally {
            Files.deleteIfExists(imageFile);
        }
    }

    @Test
    void extractsTableContent() throws IOException {
        Path pptxFile = createRichSamplePptx();
        try {
            DocumentProfile profile = new DocumentProfile(DocumentSourceType.PPTX, false, false, false, false);

            var result = extractor.extract(pptxFile, profile);

            assertTrue(result.extractedText().contains("Region | Revenue"));
            assertTrue(result.extractedText().contains("APAC | 320"));
        } finally {
            Files.deleteIfExists(pptxFile);
        }
    }

    @Test
    void extractsNotesAndImageFromProvidedSample() throws IOException {
        Path pptxFile = copyProvidedSamplePptx();
        try {
            DocumentProfile profile = new DocumentProfile(DocumentSourceType.PPTX, false, false, false, false);

            var result = extractor.extract(pptxFile, profile);

            assertTrue(result.extractedText().contains("Notes"));
            assertTrue(result.extractedText().contains("大家好，今天我将向大家汇报我们最新的数字化建设方案"));
            assertTrue(result.extractedText().contains("[Image: Picture 2]"));
        } finally {
            Files.deleteIfExists(pptxFile);
        }
    }

    @Test
    void extractsPdfPagesAndLines() throws IOException {
        Path pdfFile = createMultiPagePdf();
        try {
            DocumentProfile profile = new DocumentProfile(DocumentSourceType.PDF, false, false, false, false);

            var result = extractor.extract(pdfFile, profile);

            assertEquals(2, result.pageCount());
            assertEquals(2, result.pageTexts().size());
            assertEquals(2, result.pageLines().size());
            assertTrue(result.pageTexts().get(0).contains("PDF Page 1"));
            assertTrue(result.pageTexts().get(1).contains("PDF Page 2"));
            assertEquals("PDF Page 1", result.pageLines().get(0).get(0));
            assertEquals("Revenue stayed stable.", result.pageLines().get(0).get(1));
            assertEquals("PDF Page 2", result.pageLines().get(1).get(0));
            assertTrue(result.extractedText().contains("Margin improved."));
        } finally {
            Files.deleteIfExists(pdfFile);
        }
    }

    @Test
    void extractsPdfIntoIrWithOneContainerPerPage() throws IOException {
        Path pdfFile = createMultiPagePdf();
        try {
            DocumentProfile profile = new DocumentProfile(DocumentSourceType.PDF, false, false, false, false);
            DocumentIr ir = createIr(DocumentSourceType.PDF, pdfFile);

            new BasicPdfExtractor().extract(pdfFile, profile, ir);

            assertEquals(2, ir.getMeta().pageCount());
            assertEquals(2, ir.getContainers().size());
            assertEquals(2, ir.getNodes().size());
            assertTrue(ir.getNodes().stream().allMatch(node -> node.nodeType() == NodeType.PARAGRAPH));
            assertTrue(ir.getNodes().stream().anyMatch(node -> node.text().contains("PDF Page 1")));
            assertTrue(ir.getNodes().stream().anyMatch(node -> node.text().contains("PDF Page 2")));
            assertFalse(ir.getNodes().stream().anyMatch(node -> node.text().isBlank()));
        } finally {
            Files.deleteIfExists(pdfFile);
        }
    }

    private Path createSamplePptx() throws IOException {
        Path file = Files.createTempFile("smartdoc-flow-format-", ".pptx");
        try (XMLSlideShow slideShow = new XMLSlideShow(); OutputStream outputStream = Files.newOutputStream(file)) {
            XSLFSlide slide = slideShow.createSlide();
            XSLFTextBox title = slide.createTextBox();
            title.setText("Quarterly Review");
            XSLFTextBox body = slide.createTextBox();
            body.setText("Revenue grew by 20%\nCosts remained stable");
            slideShow.write(outputStream);
        }
        return file;
    }

    private Path createSampleDocx() throws IOException {
        Path file = Files.createTempFile("smartdoc-flow-format-", ".docx");
        try (XWPFDocument document = new XWPFDocument(); OutputStream outputStream = Files.newOutputStream(file)) {
            XWPFParagraph title = document.createParagraph();
            title.setStyle("Title");
            title.createRun().setText("Project Overview");

            XWPFParagraph body = document.createParagraph();
            body.createRun().setText("This document summarizes the current implementation status.");

            XWPFTable table = document.createTable(2, 2);
            table.getRow(0).getCell(0).setText("Metric");
            table.getRow(0).getCell(1).setText("Value");
            table.getRow(1).getCell(0).setText("Coverage");
            table.getRow(1).getCell(1).setText("Baseline");

            document.write(outputStream);
        }
        return file;
    }

    private Path createSampleXlsx() throws IOException {
        Path file = Files.createTempFile("smartdoc-flow-format-", ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(file)) {
            XSSFSheet sheet = workbook.createSheet("Summary");
            sheet.createRow(0).createCell(0).setCellValue("Metric");
            sheet.getRow(0).createCell(1).setCellValue("Value");
            sheet.createRow(1).createCell(0).setCellValue("Coverage");
            sheet.getRow(1).createCell(1).setCellValue("Baseline");
            sheet.createRow(2).createCell(0).setCellValue("Owner");
            sheet.getRow(2).createCell(1).setCellValue("Core Team");
            workbook.write(outputStream);
        }
        return file;
    }

    private Path createRichSamplePptx() throws IOException {
        Path file = Files.createTempFile("smartdoc-flow-format-rich-", ".pptx");
        try (XMLSlideShow slideShow = new XMLSlideShow(); OutputStream outputStream = Files.newOutputStream(file)) {
            XSLFSlide slide = slideShow.createSlide();

            XSLFTextBox title = slide.createTextBox();
            title.setText("Regional Snapshot");

            XSLFTable table = slide.createTable();
            table.setAnchor(new java.awt.Rectangle(40, 120, 400, 120));
            XSLFTableRow header = table.addRow();
            header.setHeight(30);
            header.addCell().setText("Region");
            header.addCell().setText("Revenue");
            XSLFTableRow data = table.addRow();
            data.setHeight(30);
            data.addCell().setText("APAC");
            data.addCell().setText("320");

            slideShow.write(outputStream);
        }
        return file;
    }

    private Path createSampleImage() throws IOException {
        Path file = Files.createTempFile("smartdoc-flow-format-", ".png");
        BufferedImage image = new BufferedImage(120, 60, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.drawRect(10, 10, 80, 20);
        } finally {
            graphics.dispose();
        }
        javax.imageio.ImageIO.write(image, "png", file.toFile());
        return file;
    }

    private Path createMultiPagePdf() throws IOException {
        Path file = Files.createTempFile("smartdoc-flow-format-", ".pdf");
        try (PDDocument document = new PDDocument()) {
            writePdfPage(document, "PDF Page 1", "Revenue stayed stable.");
            writePdfPage(document, "PDF Page 2", "Margin improved.");
            document.save(file.toFile());
        }
        return file;
    }

    private void writePdfPage(PDDocument document, String firstLine, String secondLine) throws IOException {
        PDPage page = new PDPage(PDRectangle.LETTER);
        document.addPage(page);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
            contentStream.newLineAtOffset(72, 700);
            contentStream.showText(firstLine);
            contentStream.newLineAtOffset(0, -22);
            contentStream.showText(secondLine);
            contentStream.endText();
        }
    }

    private Path copyProvidedSamplePptx() throws IOException {
        Path file = Files.createTempFile("smartdoc-flow-format-provided-", ".pptx");
        try (InputStream inputStream = getClass().getResourceAsStream("/pptx/sample-notes-image.pptx")) {
            if (inputStream == null) {
                throw new IOException("Missing test resource: /pptx/sample-notes-image.pptx");
            }
            Files.copy(inputStream, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return file;
    }

    private DocumentIr createIr(DocumentSourceType sourceType, Path source) {
        return new DocumentIr(new DocumentMeta(
            "test-doc",
            sourceType,
            source.getFileName().toString(),
            0,
            List.of(),
            false,
            false,
            false,
            false,
            "test",
            "test"
        ));
    }

}
