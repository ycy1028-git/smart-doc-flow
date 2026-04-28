package io.ycy.smartdocflow.format;

import io.ycy.smartdocflow.common.model.Bbox;
import io.ycy.smartdocflow.common.model.DocumentSourceType;
import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.model.ir.Container;
import io.ycy.smartdocflow.core.model.ir.ContainerType;
import io.ycy.smartdocflow.core.model.ir.Diagnostic;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.model.ir.Node;
import io.ycy.smartdocflow.core.model.ir.NodeType;
import io.ycy.smartdocflow.core.model.ir.SourceRef;
import io.ycy.smartdocflow.core.spi.FormatExtractionResult;
import io.ycy.smartdocflow.core.spi.FormatExtractor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public final class BasicPdfExtractor implements FormatExtractor {

    @Override
    public FormatExtractionResult extract(Path source, DocumentProfile profile) {
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

    @Override
    public void extract(Path source, DocumentProfile profile, DocumentIr ir) {
        if (profile.sourceType() != DocumentSourceType.PDF) {
            ir.addDiagnostic(new Diagnostic("EXTRACT", "reason", "skip-non-pdf-source", System.currentTimeMillis()));
            return;
        }

        ir.addDiagnostic(new Diagnostic("EXTRACT", "strategy", "pdf-text-stripper", System.currentTimeMillis()));

        try (PDDocument document = Loader.loadPDF(source.toFile())) {
            int pageCount = document.getNumberOfPages();
            updateMetaPageCount(ir, pageCount);
            ir.addDiagnostic(new Diagnostic("EXTRACT", "pageCount", pageCount, System.currentTimeMillis()));

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            int extractedPages = 0;

            for (int page = 1; page <= pageCount; page++) {
                String containerId = UUID.randomUUID().toString();
                Bbox pageBox = new Bbox(0, 0, 1000, 1400);
                ir.addContainer(new Container(
                    containerId,
                    ContainerType.PAGE,
                    page - 1,
                    "Page " + page,
                    0, 0,
                    pageBox,
                    Map.of()
                ));

                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = normalize(stripper.getText(document));

                if (!text.isBlank()) {
                    extractedPages++;
                    String nodeId = UUID.randomUUID().toString();
                    ir.addNode(new Node(
                        nodeId,
                        NodeType.PARAGRAPH,
                        containerId,
                        estimateBlockBbox(0),
                        text,
                        List.of(),
                        1.0,
                        String.valueOf(page),
                        List.of(new SourceRef("pdfTextSpan", containerId, 0, text.length())),
                        Map.of(
                            "nodeRole", "page-text",
                            "pageIndex", page - 1,
                            "pageLabel", "Page " + page
                        ),
                        java.util.Set.of("EXTRACT")
                    ));
                    ir.addRelation(new io.ycy.smartdocflow.core.model.ir.Relation(containerId, nodeId, io.ycy.smartdocflow.core.model.ir.RelationType.CHILD_OF, 1.0, Map.of("stage", "EXTRACT")));
                }
            }
            ir.addDiagnostic(new Diagnostic("EXTRACT", "extractedPages", extractedPages, System.currentTimeMillis()));
            ir.addDiagnostic(new Diagnostic("EXTRACT", "reason", extractedPages == 0 ? "no-pdf-text-extracted" : "pdf-text-extracted", System.currentTimeMillis()));
        } catch (Exception e) {
            ir.addDiagnostic(new Diagnostic("EXTRACT", "fallback", "pdf-extract-failed", System.currentTimeMillis()));
            ir.addDiagnostic(new Diagnostic("EXTRACT", "error", e.getClass().getSimpleName(), System.currentTimeMillis()));
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

    private List<String> splitLines(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(text.split("\\n", -1))
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
        double y = 80 + (orderIndex * 120);
        return new Bbox(80, y, 840, 80);
    }
}
