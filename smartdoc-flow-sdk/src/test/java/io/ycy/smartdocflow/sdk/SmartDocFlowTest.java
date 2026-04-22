package io.ycy.smartdocflow.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.ycy.smartdocflow.common.model.DocumentSourceType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
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
    void profilesAndRendersPptxFile() throws IOException {
        Path pptxFile = createRichSamplePptx();
        try {
            var profile = smartDocFlow.profile(pptxFile);
            String markdown = smartDocFlow.parseToMarkdown(pptxFile);
            String json = smartDocFlow.parseToJson(pptxFile);

            assertEquals(DocumentSourceType.PPTX, profile.sourceType());
            assertTrue(markdown.contains("Quarterly Review"));
            assertTrue(markdown.contains("Revenue grew by 20%"));
            assertTrue(markdown.contains("Region | Revenue"));
            assertTrue(json.contains("Quarterly Review"));
            assertTrue(json.contains("Revenue grew by 20%"));
            assertTrue(json.contains("Region | Revenue"));
        } finally {
            Files.deleteIfExists(pptxFile);
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

}
