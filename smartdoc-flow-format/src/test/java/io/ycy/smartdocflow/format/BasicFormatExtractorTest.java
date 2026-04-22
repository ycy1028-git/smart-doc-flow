package io.ycy.smartdocflow.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.ycy.smartdocflow.common.model.DocumentSourceType;
import io.ycy.smartdocflow.core.model.DocumentProfile;
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

class BasicFormatExtractorTest {
    private final BasicFormatExtractor extractor = new BasicFormatExtractor();

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

}
