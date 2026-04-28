package io.ycy.smartdocflow.cli;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.apache.poi.util.Units;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public final class BenchmarkSampleGenerator {
    private BenchmarkSampleGenerator() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: BenchmarkSampleGenerator <output-dir> <project-root>");
            System.exit(1);
        }

        Path outputDir = Path.of(args[0]);
        Path projectRoot = Path.of(args[1]);
        Files.createDirectories(outputDir);

        Path imageFile = outputDir.resolve("ocr-sample.png");
        writeImageSample(imageFile);
        writeBlankImageSample(outputDir.resolve("blank-image.png"));
        writeTextPdfSample(outputDir.resolve("text-sample.pdf"));
        writeComplexTextPdfSample(outputDir.resolve("complex-text-sample.pdf"));
        writeScannedPdfSample(outputDir.resolve("scanned-sample.pdf"), imageFile);
        writeBlankPdfSample(outputDir.resolve("blank.pdf"));
        writeDocxSample(outputDir.resolve("sample.docx"));
        writeDocxWithImageSample(outputDir.resolve("sample-image.docx"), imageFile);
        writeBlankDocxSample(outputDir.resolve("blank.docx"));
        writeCorruptDocxSample(outputDir.resolve("broken.docx"));
        writeXlsxSample(outputDir.resolve("sample.xlsx"));
        writeBlankXlsxSample(outputDir.resolve("blank.xlsx"));
        writeCorruptXlsxSample(outputDir.resolve("broken.xlsx"));
        writePptxSample(outputDir.resolve("sample.pptx"));
        writeBlankPptxSample(outputDir.resolve("blank.pptx"));
        writeCorruptPptxSample(outputDir.resolve("broken.pptx"));
        copyProvidedPptxSample(outputDir.resolve("sample-notes-image.pptx"), projectRoot);
        writeCorruptPdfSample(outputDir.resolve("broken.pdf"));
    }

    private static void writeImageSample(Path file) throws IOException {
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
    }

    private static void writeTextPdfSample(Path file) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("Text PDF Example");
                contentStream.endText();
            }
            document.save(file.toFile());
        }
    }

    private static void writeComplexTextPdfSample(Path file) throws IOException {
        try (PDDocument document = new PDDocument()) {
            writePdfPage(document, "Quarterly Review", new String[]{"Revenue grew by 20%.", "Margin stayed above 35%."});
            writePdfPage(document, "Operational Risks", new String[]{"Supplier delay risk remains medium.", "Mitigation plan is active."});
            document.save(file.toFile());
        }
    }

    private static void writeBlankImageSample(Path file) throws IOException {
        BufferedImage image = new BufferedImage(320, 120, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            graphics.dispose();
        }
        javax.imageio.ImageIO.write(image, "png", file.toFile());
    }

    private static void writePdfPage(PDDocument document, String title, String[] lines) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
            contentStream.newLineAtOffset(50, 700);
            contentStream.showText(title);
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            for (String line : lines) {
                contentStream.newLineAtOffset(0, -22);
                contentStream.showText(line);
            }
            contentStream.endText();
        }
    }

    private static void writeBlankPdfSample(Path file) throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(PDRectangle.A4));
            document.save(file.toFile());
        }
    }

    private static void writeScannedPdfSample(Path file, Path imageFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            BufferedImage image = javax.imageio.ImageIO.read(imageFile.toFile());
            PDImageXObject imageObject = LosslessFactory.createFromImage(document, image);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(imageObject, 40, 600, 240, 90);
            }
            document.save(file.toFile());
        }
    }

    private static void writeDocxSample(Path file) throws IOException {
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
    }

    private static void writeDocxWithImageSample(Path file, Path imageFile) throws IOException {
        try (XWPFDocument document = new XWPFDocument(); OutputStream outputStream = Files.newOutputStream(file); InputStream imageStream = Files.newInputStream(imageFile)) {
            XWPFParagraph title = document.createParagraph();
            title.setStyle("Title");
            title.createRun().setText("Project Overview");

            XWPFParagraph imageParagraph = document.createParagraph();
            XWPFRun run = imageParagraph.createRun();
            run.addPicture(imageStream, XWPFDocument.PICTURE_TYPE_PNG, imageFile.getFileName().toString(), Units.toEMU(120), Units.toEMU(60));

            document.write(outputStream);
        } catch (Exception e) {
            throw new IOException("failed to write docx image sample", e);
        }
    }

    private static void writeBlankDocxSample(Path file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(); OutputStream outputStream = Files.newOutputStream(file)) {
            document.createParagraph();
            document.write(outputStream);
        }
    }

    private static void writeCorruptDocxSample(Path file) throws IOException {
        Files.writeString(file, "not-a-real-docx");
    }

    private static void writeXlsxSample(Path file) throws IOException {
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
    }

    private static void writeBlankXlsxSample(Path file) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(file)) {
            workbook.createSheet("Blank");
            workbook.write(outputStream);
        }
    }

    private static void writeCorruptXlsxSample(Path file) throws IOException {
        Files.writeString(file, "not-a-real-xlsx");
    }

    private static void writePptxSample(Path file) throws IOException {
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
    }

    private static void writeBlankPptxSample(Path file) throws IOException {
        try (XMLSlideShow slideShow = new XMLSlideShow(); OutputStream outputStream = Files.newOutputStream(file)) {
            slideShow.createSlide();
            slideShow.write(outputStream);
        }
    }

    private static void writeCorruptPptxSample(Path file) throws IOException {
        Files.writeString(file, "not-a-real-pptx");
    }

    private static void writeCorruptPdfSample(Path file) throws IOException {
        Files.writeString(file, "not-a-real-pdf");
    }

    private static void copyProvidedPptxSample(Path file, Path projectRoot) throws IOException {
        Path source = projectRoot.resolve(Path.of("smartdoc-flow-format", "src", "test", "resources", "pptx", "sample-notes-image.pptx"));
        Files.copy(source, file, StandardCopyOption.REPLACE_EXISTING);
    }
}
