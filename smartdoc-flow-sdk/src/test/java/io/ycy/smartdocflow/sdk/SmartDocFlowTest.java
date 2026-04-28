package io.ycy.smartdocflow.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
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
        assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals("PIPELINE") && diagnostic.key().equals("multiColumn")));
        assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals("EXTRACT") && diagnostic.key().equals("started")));
        assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals("EXTRACT") && diagnostic.key().equals("durationMs")));
        assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals("EXTRACT") && diagnostic.key().equals("nodeDelta")));
        assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals("POST") && diagnostic.key().equals("afterNodes")));
    }

    @Test
    void rejectsMissingInputFile() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> smartDocFlow.parse(Path.of("missing-file.txt")));

        assertTrue(exception.getMessage().contains("输入文件不存在:"));
    }

    @Test
    void rejectsDirectoryInput() throws IOException {
        Path directory = Files.createTempDirectory("smartdoc-flow-sdk-dir-");
        try {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> smartDocFlow.parse(directory));

            assertTrue(exception.getMessage().contains("输入路径不是文件:"));
        } finally {
            Files.deleteIfExists(directory);
        }
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
            assertHasStageDiagnostics(diagnostics, "EXTRACT", "strategy");
            assertHasStageDiagnostics(diagnostics, "EXTRACT", "slideCount");
            assertHasStageDiagnostics(diagnostics, "EXTRACT", "extractedSlides");
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
            assertHasStageDiagnostics(diagnostics, "EXTRACT", "strategy");
            assertHasStageDiagnostics(diagnostics, "EXTRACT", "paragraphNodes");
            assertHasStageDiagnostics(diagnostics, "EXTRACT", "tableNodes");
            assertHasStageDiagnostics(diagnostics, "NORMALIZE", "afterNodes");
            assertHasStageDiagnostics(diagnostics, "POST", "afterNodes");
        } finally {
            Files.deleteIfExists(docxFile);
        }
    }

    @Test
    void parsesDocxAssetsWhenImageExists() throws Exception {
        Path docxFile = createSampleDocxWithImage();
        try {
            var result = smartDocFlow.parse(docxFile);
            String markdown = smartDocFlow.parseToMarkdown(docxFile);
            String json = smartDocFlow.parseToJson(docxFile);

            assertFalse(result.assets().isEmpty());
            assertContainsExpectedFragments(markdown, "/expected/docx/sample-with-image.markdown.txt");
            assertContainsExpectedFragments(json, "/expected/docx/sample-with-image.json.txt");
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
            assertHasStageDiagnostics(diagnostics, "EXTRACT", "strategy");
            assertHasStageDiagnostics(diagnostics, "EXTRACT", "sheetCount");
            assertHasStageDiagnostics(diagnostics, "EXTRACT", "extractedSheets");
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
            assertHasStageDiagnostics(diagnostics, "EXTRACT", "strategy");
            assertHasStageDiagnostics(diagnostics, "EXTRACT", "pageCount");
            assertHasStageDiagnostics(diagnostics, "EXTRACT", "extractedPages");
            assertHasStageDiagnostics(diagnostics, "EXTRACT", "afterNodes");
            assertHasStageDiagnostics(diagnostics, "OCR", "afterNodes");
            assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("route") && "fallback".equals(String.valueOf(diagnostic.value()))));
            assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("fallback") && "no-route-matched".equals(String.valueOf(diagnostic.value()))));
        } finally {
            Files.deleteIfExists(pdfFile);
        }
    }

    @Test
    void rendersNotesAndImageFromProvidedSample() throws IOException {
        Path pptxFile = copyProvidedSamplePptx();
        try {
            var result = smartDocFlow.parse(pptxFile);
            String markdown = smartDocFlow.parseToMarkdown(pptxFile);
            String json = smartDocFlow.parseToJson(pptxFile);

            assertFalse(result.assets().isEmpty());
            assertContainsExpectedFragments(markdown, "/expected/pptx/sample-notes-image.markdown.txt");
            assertContainsExpectedFragments(json, "/expected/pptx/sample-notes-image.json.txt");
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
                assertContainsExpectedFragments(markdown, "/expected/image/ocr-sample.available.markdown.txt");
                assertContainsExpectedFragments(json, "/expected/image/ocr-sample.available.json.txt");
                assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("result")));
            } else {
                assertTrue(result.blocks().isEmpty());
                assertContainsExpectedFragments(markdown, "/expected/image/ocr-sample.degraded.markdown.txt", imageFile.getFileName().toString());
                assertContainsExpectedFragments(json, "/expected/image/ocr-sample.degraded.json.txt");
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
                assertContainsExpectedFragments(markdown, "/expected/pdf/scanned-pdf.available.markdown.txt");
                assertContainsExpectedFragments(json, "/expected/pdf/scanned-pdf.available.json.txt");
                assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("result")));
                assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("pageCount")));
                assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("page.0.result")));
                assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("page.0.preprocess")));
            } else {
                assertTrue(result.blocks().isEmpty());
                assertContainsExpectedFragments(markdown, "/expected/pdf/scanned-pdf.degraded.markdown.txt", pdfFile.getFileName().toString());
                assertContainsExpectedFragments(json, "/expected/pdf/scanned-pdf.degraded.json.txt");
                assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals("OCR") && diagnostic.key().equals("backend") && "tesseract-not-found".equals(String.valueOf(diagnostic.value()))));
            }
        } finally {
            Files.deleteIfExists(pdfFile);
        }
    }

    @Test
    void runsRegressionEntryAcrossStableFormats() throws Exception {
        List<Path> createdFiles = new ArrayList<>();
        try {
            Path pdfFile = createTextPdf();
            Path complexPdfFile = createComplexTextPdf();
            Path docxFile = createSampleDocx();
            Path docxImageFile = createSampleDocxWithImage();
            Path xlsxFile = createSampleXlsx();
            Path pptxFile = createRichSamplePptx();
            createdFiles.add(pdfFile);
            createdFiles.add(complexPdfFile);
            createdFiles.add(docxFile);
            createdFiles.add(docxImageFile);
            createdFiles.add(xlsxFile);
            createdFiles.add(pptxFile);

            assertStableOutputs(pdfFile, "PDF", "/expected/pdf/text-pdf.markdown.txt", "/expected/pdf/text-pdf.json.txt");
            assertStableOutputs(complexPdfFile, "PDF", "/expected/pdf/complex-text-pdf.markdown.txt", "/expected/pdf/complex-text-pdf.json.txt");
            assertStableOutputs(docxFile, "DOCX", "/expected/docx/sample.markdown.txt", "/expected/docx/sample.json.txt");
            assertStableOutputs(xlsxFile, "XLSX", "/expected/xlsx/sample.markdown.txt", "/expected/xlsx/sample.json.txt");
            assertStableOutputs(pptxFile, "PPTX", "/expected/pptx/rich-sample.markdown.txt", "/expected/pptx/rich-sample.json.txt");
        } finally {
            for (Path createdFile : createdFiles) {
                Files.deleteIfExists(createdFile);
            }
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

    private Path createSampleDocxWithImage() throws Exception {
        Path file = Files.createTempFile("smartdoc-flow-sdk-img-", ".docx");
        Path image = createOcrSampleImage();
        try (XWPFDocument document = new XWPFDocument(); OutputStream outputStream = Files.newOutputStream(file); InputStream imageStream = Files.newInputStream(image)) {
            XWPFParagraph title = document.createParagraph();
            title.setStyle("Title");
            title.createRun().setText("Project Overview");

            XWPFParagraph imageParagraph = document.createParagraph();
            XWPFRun run = imageParagraph.createRun();
            run.addPicture(imageStream, XWPFDocument.PICTURE_TYPE_PNG, image.getFileName().toString(), Units.toEMU(120), Units.toEMU(60));

            document.write(outputStream);
        } finally {
            Files.deleteIfExists(image);
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

    private Path createComplexTextPdf() throws IOException {
        Path file = Files.createTempFile("smartdoc-flow-sdk-complex-text-", ".pdf");
        try (PDDocument document = new PDDocument()) {
            writePdfPage(document, "Quarterly Review", List.of("Revenue grew by 20%.", "Margin stayed above 35%."));
            writePdfPage(document, "Operational Risks", List.of("Supplier delay risk remains medium.", "Mitigation plan is active."));
            document.save(file.toFile());
        }
        return file;
    }

    private void writePdfPage(PDDocument document, String title, List<String> lines) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.beginText();
            contentStream.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD), 14);
            contentStream.newLineAtOffset(50, 700);
            contentStream.showText(title);
            contentStream.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 12);
            for (String line : lines) {
                contentStream.newLineAtOffset(0, -22);
                contentStream.showText(line);
            }
            contentStream.endText();
        }
    }

    private void assertHasStageDiagnostics(java.util.List<io.ycy.smartdocflow.core.model.ir.Diagnostic> diagnostics, String stage, String key) {
        assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.stage().equals(stage) && diagnostic.key().equals(key)));
    }

    private void assertContainsExpectedFragments(String actual, String resourcePath) throws IOException {
        assertContainsExpectedFragments(actual, resourcePath, null);
    }

    private void assertContainsExpectedFragments(String actual, String resourcePath, String dynamicValue) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Missing test resource: " + resourcePath);
            }
            for (String fragment : new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).lines().toList()) {
                String trimmed = fragment.trim().replace("{{fileName}}", dynamicValue == null ? "" : dynamicValue);
                if (!trimmed.isEmpty()) {
                    assertTrue(actual.contains(trimmed), "Missing expected fragment: " + trimmed);
                }
            }
        }
    }

    private void assertStableOutputs(Path source, String expectedSourceType, String markdownExpectedResource, String jsonExpectedResource) throws IOException {
        var profile = smartDocFlow.profile(source);
        var diagnostics = smartDocFlow.parseDiagnostics(source);
        String markdown = smartDocFlow.parseToMarkdown(source);
        String json = smartDocFlow.parseToJson(source);

        assertEquals(expectedSourceType, profile.sourceType().name());
        assertTrue(markdown.startsWith("# " + source.getFileName()));
        assertContainsExpectedFragments(markdown, markdownExpectedResource);
        assertTrue(json.contains("\"documentId\":"));
        assertTrue(json.contains("\"fileName\":\"" + source.getFileName() + "\""));
        assertTrue(json.contains("\"blocks\":["));
        assertTrue(json.contains("\"id\":"));
        assertTrue(json.contains("\"type\":"));
        assertTrue(json.contains("\"page\":"));
        assertTrue(json.contains("\"order\":"));
        assertTrue(json.contains("\"text\":"));
        assertContainsExpectedFragments(json, jsonExpectedResource);
        assertMarkdownRules(markdown, expectedSourceType);
        assertJsonRules(json, expectedSourceType);
        assertJsonSchema(json);
        assertHasStageDiagnostics(diagnostics, "EXTRACT", "started");
        assertHasStageDiagnostics(diagnostics, "EXTRACT", "afterNodes");
        assertHasStageDiagnostics(diagnostics, "POST", "completed");
    }

    private void assertMarkdownRules(String markdown, String expectedSourceType) {
        if (expectedSourceType.equals("DOCX") || expectedSourceType.equals("PPTX")) {
            assertTrue(markdown.contains("## "), "Expected heading/title markdown block");
        }
        if (expectedSourceType.equals("DOCX") || expectedSourceType.equals("XLSX") || expectedSourceType.equals("PPTX")) {
            assertTrue(markdown.contains("```text"), "Expected fenced table block");
            assertTrue(markdown.contains("```"), "Expected closing fenced block");
        }
    }

    private void assertJsonRules(String json, String expectedSourceType) {
        assertTrue(json.contains("\"order\":0"), "Expected first block order");
        if (expectedSourceType.equals("PDF")) {
            assertTrue(json.contains("\"page\":0"), "Expected first page index");
            assertTrue(
                json.contains("\"type\":\"PARAGRAPH\"") || json.contains("\"type\":\"HEADING\"") || json.contains("\"type\":\"TITLE\""),
                "Expected text-like block type"
            );
        }
        if (expectedSourceType.equals("DOCX") || expectedSourceType.equals("PPTX")) {
            assertTrue(json.contains("\"type\":\"TITLE\"") || json.contains("\"type\":\"HEADING\""), "Expected heading-like block type");
        }
        if (expectedSourceType.equals("DOCX") || expectedSourceType.equals("XLSX") || expectedSourceType.equals("PPTX")) {
            assertTrue(json.contains("\"type\":\"TABLE\""), "Expected table block type");
        }
    }

    private void assertJsonSchema(String json) {
        assertTrue(json.startsWith("{"), "JSON should start with object");
        assertTrue(json.endsWith("}"), "JSON should end with object");
        assertTrue(json.contains("\"documentId\":\""), "Missing documentId string field");
        assertTrue(json.contains("\"fileName\":\""), "Missing fileName string field");
        assertTrue(json.contains("\"blocks\":["), "Missing blocks array field");

        List<String> blockEntries = extractBlockEntries(json);
        Set<Integer> seenOrders = new HashSet<>();
        for (int index = 0; index < blockEntries.size(); index++) {
            String block = blockEntries.get(index);
            assertTrue(block.startsWith("{"), "Block should start with object");
            assertTrue(block.endsWith("}"), "Block should end with object");
            assertTrue(block.contains("\"id\":\""), "Block missing id");
            assertTrue(block.contains("\"type\":\""), "Block missing type");
            assertTrue(block.contains("\"page\":"), "Block missing page");
            assertTrue(block.contains("\"order\":"), "Block missing order");
            assertTrue(block.contains("\"text\":\""), "Block missing text");

            int order = parseIntField(block, "order");
            int page = parseIntField(block, "page");
            assertEquals(index, order, "Block order should be continuous from zero");
            assertTrue(page >= 0, "Block page should be non-negative");
            assertTrue(seenOrders.add(order), "Block order should be unique");
        }
    }

    private List<String> extractBlockEntries(String json) {
        int blocksStart = json.indexOf("\"blocks\":[");
        assertTrue(blocksStart >= 0, "Missing blocks array");
        int arrayStart = json.indexOf('[', blocksStart);
        int arrayEnd = json.lastIndexOf(']');
        assertTrue(arrayStart >= 0 && arrayEnd >= arrayStart, "Invalid blocks array bounds");
        String content = json.substring(arrayStart + 1, arrayEnd).trim();
        if (content.isEmpty()) {
            return List.of();
        }

        List<String> blocks = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int index = 0; index < content.length(); index++) {
            char ch = content.charAt(index);
            if (ch == '{') {
                if (depth == 0) {
                    start = index;
                }
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    blocks.add(content.substring(start, index + 1));
                }
            }
        }
        return blocks;
    }

    private int parseIntField(String jsonObject, String fieldName) {
        String marker = "\"" + fieldName + "\":";
        int start = jsonObject.indexOf(marker);
        assertTrue(start >= 0, "Missing field: " + fieldName);
        start += marker.length();
        int end = start;
        while (end < jsonObject.length() && Character.isDigit(jsonObject.charAt(end))) {
            end++;
        }
        return Integer.parseInt(jsonObject.substring(start, end));
    }

    private boolean isTesseractAvailable() {
        String configured = System.getenv("SMARTDOC_FLOW_TESSERACT_PATH");
        if (configured != null && !configured.isBlank()) {
            return Files.isExecutable(Path.of(configured));
        }
        return Files.isExecutable(Path.of("/opt/homebrew/bin/tesseract"));
    }

}
