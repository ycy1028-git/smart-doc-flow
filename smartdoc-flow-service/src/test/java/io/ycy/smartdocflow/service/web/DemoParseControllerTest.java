package io.ycy.smartdocflow.service.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DemoParseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void parsesUploadedFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "sample.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "财务摘要\n\n整体稳定。".getBytes()
        );

        mockMvc.perform(multipart("/api/demo/parse").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fileName").value("sample.txt"))
            .andExpect(jsonPath("$.sourceType").value("UNKNOWN"))
            .andExpect(jsonPath("$.markdown").isString())
            .andExpect(jsonPath("$.json").isString())
            .andExpect(jsonPath("$.diagnostics").isArray())
            .andExpect(jsonPath("$.diagnostics[0].stage").value("PIPELINE"));
    }

    @Test
    void parsesUploadedPdfAndReturnsDiagnostics() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "sample.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            createTextPdfBytes()
        );

        mockMvc.perform(multipart("/api/demo/parse").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sourceType").value("PDF"))
            .andExpect(jsonPath("$.markdown").value(org.hamcrest.Matchers.containsString("Text PDF Example")))
            .andExpect(jsonPath("$.diagnostics[?(@.stage == 'EXTRACT' && @.key == 'afterNodes')]").isNotEmpty())
            .andExpect(jsonPath("$.diagnostics[?(@.stage == 'OCR' && @.key == 'afterNodes')]").isNotEmpty());
    }

    @Test
    void parsesUploadedDocxAndReturnsOfficeContent() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "sample.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            createDocxBytes()
        );

        mockMvc.perform(multipart("/api/demo/parse").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sourceType").value("DOCX"))
            .andExpect(jsonPath("$.markdown").value(org.hamcrest.Matchers.containsString("Project Overview")))
            .andExpect(jsonPath("$.markdown").value(org.hamcrest.Matchers.containsString("Metric | Value")))
            .andExpect(jsonPath("$.diagnostics[?(@.stage == 'NORMALIZE' && @.key == 'afterNodes')]").isNotEmpty());
    }

    @Test
    void parsesUploadedXlsxAndReturnsOfficeContent() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "sample.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            createXlsxBytes()
        );

        mockMvc.perform(multipart("/api/demo/parse").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sourceType").value("XLSX"))
            .andExpect(jsonPath("$.markdown").value(org.hamcrest.Matchers.containsString("Summary")))
            .andExpect(jsonPath("$.json").value(org.hamcrest.Matchers.containsString("Coverage | Baseline")))
            .andExpect(jsonPath("$.diagnostics[?(@.stage == 'TABLE_RECOVER' && @.key == 'normalizedTables')]").isNotEmpty());
    }

    @Test
    void parsesUploadedImageAndReturnsOcrDiagnostics() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "sample.png",
            MediaType.IMAGE_PNG_VALUE,
            createImageBytes()
        );

        mockMvc.perform(multipart("/api/demo/parse").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sourceType").value("IMAGE"))
            .andExpect(jsonPath("$.imageHeavy").value(true))
            .andExpect(jsonPath("$.diagnostics[?(@.stage == 'OCR')]").isNotEmpty());
    }

    @Test
    void rejectsEmptyUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "empty.txt",
            MediaType.TEXT_PLAIN_VALUE,
            new byte[0]
        );

        mockMvc.perform(multipart("/api/demo/parse").file(file))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("上传文件不能为空"));
    }

    private byte[] createTextPdfBytes() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                contentStream.newLineAtOffset(72, 700);
                contentStream.showText("Text PDF Example");
                contentStream.newLineAtOffset(0, -22);
                contentStream.showText("Quarterly revenue stayed stable.");
                contentStream.endText();
            }
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createDocxBytes() throws IOException {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var title = document.createParagraph();
            title.setStyle("Title");
            title.createRun().setText("Project Overview");

            var body = document.createParagraph();
            body.createRun().setText("This document summarizes the current implementation status.");

            XWPFTable table = document.createTable(2, 2);
            table.getRow(0).getCell(0).setText("Metric");
            table.getRow(0).getCell(1).setText("Value");
            table.getRow(1).getCell(0).setText("Coverage");
            table.getRow(1).getCell(1).setText("Baseline");

            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createXlsxBytes() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Summary");
            sheet.createRow(0).createCell(0).setCellValue("Metric");
            sheet.getRow(0).createCell(1).setCellValue("Value");
            sheet.createRow(1).createCell(0).setCellValue("Coverage");
            sheet.getRow(1).createCell(1).setCellValue("Baseline");
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createImageBytes() throws IOException {
        BufferedImage image = new BufferedImage(240, 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font("SansSerif", Font.BOLD, 24));
            graphics.drawString("OCR", 24, 48);
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        }
    }
}
