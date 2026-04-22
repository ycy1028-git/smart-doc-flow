package io.ycy.smartdocflow.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.ycy.smartdocflow.common.model.DocumentSourceType;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.junit.jupiter.api.Test;

class SmartDocFlowTest {
    private final SmartDocFlow smartDocFlow = new SmartDocFlow();
    private final Path sampleFile = Path.of("..", "sample.txt").normalize();

    @Test
    void profilesSampleTextFile() {
        var profile = smartDocFlow.profile(sampleFile);

        assertEquals(DocumentSourceType.UNKNOWN, profile.sourceType());
        assertFalse(profile.scanned());
        assertFalse(profile.multiColumn());
        assertFalse(profile.tableHeavy());
        assertFalse(profile.imageHeavy());
    }

    @Test
    void rendersMarkdownForSampleTextFile() {
        String markdown = smartDocFlow.parseToMarkdown(sampleFile);

        assertTrue(markdown.contains("# sample.txt"));
        assertTrue(markdown.contains("SmartDoc-Flow skeleton extracted content for sample.txt"));
    }

    @Test
    void rendersJsonForSampleTextFile() {
        String json = smartDocFlow.parseToJson(sampleFile);

        assertTrue(json.contains("\"fileName\":\"sample.txt\""));
        assertTrue(json.contains("\"blocks\""));
    }

    @Test
    void parsesSampleTextFileIntoDocumentResult() {
        var result = smartDocFlow.parse(sampleFile);

        assertNotNull(result.metadata());
        assertEquals("sample.txt", result.metadata().fileName());
        assertEquals(DocumentSourceType.UNKNOWN, result.metadata().sourceType());
        assertFalse(result.blocks().isEmpty());
    }

    @Test
    void exposesDiagnosticsForSampleTextFile() {
        var diagnostics = smartDocFlow.parseDiagnostics(sampleFile);

        assertFalse(diagnostics.isEmpty());
        assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals("PIPELINE") && diagnostic.key().equals("sourceType")));
        assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals("POST") && diagnostic.key().equals("afterNodes")));
    }

    @Test
    void profilesAndRendersPptxFile() throws IOException {
        Path pptxFile = createRichSamplePptx();
        try {
            var profile = smartDocFlow.profile(pptxFile);
            var result = smartDocFlow.parse(pptxFile);
            var diagnostics = smartDocFlow.parseDiagnostics(pptxFile);
            String markdown = smartDocFlow.parseToMarkdown(pptxFile);
            String json = smartDocFlow.parseToJson(pptxFile);

            assertEquals(DocumentSourceType.PPTX, profile.sourceType());
            assertFalse(result.blocks().isEmpty());
            assertTrue(markdown.contains("Quarterly Review"));
            assertTrue(markdown.contains("Revenue grew by 20%"));
            assertTrue(markdown.contains("Region | Revenue"));
            assertTrue(json.contains("Quarterly Review"));
            assertTrue(json.contains("Revenue grew by 20%"));
            assertTrue(json.contains("Region | Revenue"));
            assertHasStageDiagnostics(diagnostics, "EXTRACT", "afterNodes");
            assertHasStageDiagnostics(diagnostics, "TABLE_RECOVER", "afterNodes");
        } finally {
            Files.deleteIfExists(pptxFile);
        }
    }

    @Test
    void profilesAndRendersDocxFile() throws IOException {
        Path docxFile = createSampleDocx();
        try {
            var profile = smartDocFlow.profile(docxFile);
            var result = smartDocFlow.parse(docxFile);
            var diagnostics = smartDocFlow.parseDiagnostics(docxFile);
            String markdown = smartDocFlow.parseToMarkdown(docxFile);
            String json = smartDocFlow.parseToJson(docxFile);

            assertEquals(DocumentSourceType.DOCX, profile.sourceType());
            assertFalse(result.blocks().isEmpty());
            assertTrue(markdown.contains("Project Overview"));
            assertTrue(markdown.contains("This document summarizes the current implementation status."));
            assertTrue(markdown.contains("Metric | Value"));
            assertTrue(json.contains("Project Overview"));
            assertTrue(json.contains("This document summarizes the current implementation status."));
            assertTrue(json.contains("Metric | Value"));
            assertHasStageDiagnostics(diagnostics, "NORMALIZE", "afterNodes");
            assertHasStageDiagnostics(diagnostics, "POST", "afterNodes");
        } finally {
            Files.deleteIfExists(docxFile);
        }
    }

    @Test
    void profilesAndRendersXlsxFile() throws IOException {
        Path xlsxFile = createSampleXlsx();
        try {
            var profile = smartDocFlow.profile(xlsxFile);
            var result = smartDocFlow.parse(xlsxFile);
            var diagnostics = smartDocFlow.parseDiagnostics(xlsxFile);
            String markdown = smartDocFlow.parseToMarkdown(xlsxFile);
            String json = smartDocFlow.parseToJson(xlsxFile);

            assertEquals(DocumentSourceType.XLSX, profile.sourceType());
            assertFalse(result.blocks().isEmpty());
            assertTrue(markdown.contains("Summary"));
            assertTrue(markdown.contains("Metric | Value"));
            assertTrue(markdown.contains("Coverage | Baseline"));
            assertTrue(json.contains("Summary"));
            assertTrue(json.contains("Metric | Value"));
            assertTrue(json.contains("Coverage | Baseline"));
            assertHasStageDiagnostics(diagnostics, "EXTRACT", "afterNodes");
            assertHasStageDiagnostics(diagnostics, "TABLE_RECOVER", "normalizedTables");
        } finally {
            Files.deleteIfExists(xlsxFile);
        }
    }

    @Test
    void handlesTextPdfPathWithoutOcrFallback() throws IOException {
        Path pdfFile = createTextPdf();
        try {
            var profile = smartDocFlow.profile(pdfFile);
            var result = smartDocFlow.parse(pdfFile);
            var diagnostics = smartDocFlow.parseDiagnostics(pdfFile);
            String markdown = smartDocFlow.parseToMarkdown(pdfFile);

            assertEquals(DocumentSourceType.PDF, profile.sourceType());
            assertFalse(profile.scanned());
            assertFalse(result.blocks().isEmpty());
            assertTrue(markdown.contains("Text PDF Example"));
            assertHasStageDiagnostics(diagnostics, "EXTRACT", "afterNodes");
            assertHasStageDiagnostics(diagnostics, "OCR", "afterNodes");
            assertFalse(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("backend")));
        } finally {
            Files.deleteIfExists(pdfFile);
        }
    }

    @Test
    void rendersNotesAndImageFromProvidedSample() throws IOException {
        Path pptxFile = copyProvidedSamplePptx();
        try {
            String markdown = smartDocFlow.parseToMarkdown(pptxFile);
            String json = smartDocFlow.parseToJson(pptxFile);

            assertTrue(markdown.contains("Notes"));
            assertTrue(markdown.contains("大家好，今天我将向大家汇报我们最新的数字化建设方案"));
            assertTrue(markdown.contains("[Image: Picture 2]"));
            assertTrue(json.contains("Notes"));
            assertTrue(json.contains("大家好，今天我将向大家汇报我们最新的数字化建设方案"));
            assertTrue(json.contains("[Image: Picture 2]"));
        } finally {
            Files.deleteIfExists(pptxFile);
        }
    }

    @Test
    void handlesImageOcrPath() throws IOException {
        Path imageFile = createOcrSampleImage();
        try {
            var profile = smartDocFlow.profile(imageFile);
            var result = smartDocFlow.parse(imageFile);
            var diagnostics = smartDocFlow.parseDiagnostics(imageFile);
            String markdown = smartDocFlow.parseToMarkdown(imageFile);
            String json = smartDocFlow.parseToJson(imageFile);

            assertEquals(DocumentSourceType.IMAGE, profile.sourceType());
            assertTrue(profile.imageHeavy());

            if (isTesseractAvailable()) {
                assertFalse(result.blocks().isEmpty());
                assertTrue(markdown.length() > imageFile.getFileName().toString().length());
                assertTrue(json.contains("\"blocks\":[{"));
                assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("result")));
            } else {
                assertTrue(result.blocks().isEmpty());
                assertEquals("# " + imageFile.getFileName(), markdown.trim());
                assertTrue(json.contains("\"blocks\":[]"));
                assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("backend") && "tesseract-not-found".equals(String.valueOf(diagnostic.value()))));
            }
        } finally {
            Files.deleteIfExists(imageFile);
        }
    }

    @Test
    void handlesScannedPdfOcrPath() throws IOException {
        Path pdfFile = createScannedPdf();
        try {
            var profile = smartDocFlow.profile(pdfFile);
            var result = smartDocFlow.parse(pdfFile);
            var diagnostics = smartDocFlow.parseDiagnostics(pdfFile);
            String markdown = smartDocFlow.parseToMarkdown(pdfFile);
            String json = smartDocFlow.parseToJson(pdfFile);

            assertEquals(DocumentSourceType.PDF, profile.sourceType());
            assertTrue(profile.scanned());

            if (isTesseractAvailable()) {
                assertFalse(result.blocks().isEmpty());
                assertTrue(markdown.length() > pdfFile.getFileName().toString().length());
                assertTrue(json.contains("\"blocks\":[{"));
                assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("result")));
            } else {
                assertTrue(result.blocks().isEmpty());
                assertEquals("# " + pdfFile.getFileName(), markdown.trim());
                assertTrue(json.contains("\"blocks\":[]"));
                assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("backend") && "tesseract-not-found".equals(String.valueOf(diagnostic.value()))));
            }
        } finally {
            Files.deleteIfExists(pdfFile);
        }
    }

    private Path createRichSamplePptx() throws IOException {
        Path file = Files.createTempFile("smartdoc-flow-sdk-", ".pptx");
        try (XMLSlideShow slideShow = new XMLSlideShow(); OutputStream outputStream = Files.newOutputStream(file)) {
            XSLFSlide slide = slideShow.createSlide();
            XSLFTextBox title = slide.createTextBox();
            title.setText("Quarterly Review");
            XSLFTextBox body = slide.createTextBox();
            body.setText("Revenue grew by 20%");

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

    private Path copyProvidedSamplePptx() throws IOException {
        Path file = Files.createTempFile("smartdoc-flow-sdk-provided-", ".pptx");
        try (InputStream inputStream = getClass().getResourceAsStream("/pptx/sample-notes-image.pptx")) {
            if (inputStream == null) {
                throw new IOException("Missing test resource: /pptx/sample-notes-image.pptx");
            }
            Files.copy(inputStream, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return file;
    }

    private Path createSampleDocx() throws IOException {
        Path file = Files.createTempFile("smartdoc-flow-sdk-", ".docx");
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
        Path file = Files.createTempFile("smartdoc-flow-sdk-", ".xlsx");
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

    private Path createOcrSampleImage() throws IOException {
        Path file = Files.createTempFile("smartdoc-flow-sdk-ocr-", ".png");
        BufferedImage image = new BufferedImage(320, 120, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font("SansSerif", Font.BOLD, 28));
            graphics.drawString("OCR SAMPLE", 30, 70);
        } finally {
            graphics.dispose();
        }
        javax.imageio.ImageIO.write(image, "png", file.toFile());
        return file;
    }

    private Path createScannedPdf() throws IOException {
        Path file = Files.createTempFile("smartdoc-flow-sdk-scanned-", ".pdf");
        Path imageFile = createOcrSampleImage();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            BufferedImage image = javax.imageio.ImageIO.read(imageFile.toFile());
            PDImageXObject imageObject = LosslessFactory.createFromImage(document, image);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(imageObject, 40, 600, 240, 90);
            }
            document.save(file.toFile());
        } finally {
            Files.deleteIfExists(imageFile);
        }
        return file;
    }

    private Path createTextPdf() throws IOException {
        Path file = Files.createTempFile("smartdoc-flow-sdk-text-", ".pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("Text PDF Example");
                contentStream.endText();
            }
            document.save(file.toFile());
        }
        return file;
    }

    private void assertHasStageDiagnostics(java.util.List<io.ycy.smartdocflow.core.model.ir.Diagnostic> diagnostics, String stage, String key) {
        assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals(stage) && diagnostic.key().equals(key)));
    }

    private boolean isTesseractAvailable() {
        String configured = System.getenv("SMARTDOC_FLOW_TESSERACT_PATH");
        if (configured != null && !configured.isBlank()) {
            return Files.isExecutable(Path.of(configured));
        }
        return Files.isExecutable(Path.of("/opt/homebrew/bin/tesseract"));
    }

}
