package io.ycy.smartdocflow.layout;

import io.ycy.smartdocflow.common.model.Bbox;
import io.ycy.smartdocflow.common.model.DocumentSourceType;
import io.ycy.smartdocflow.core.model.BlockType;
import io.ycy.smartdocflow.core.model.DocumentBlock;
import io.ycy.smartdocflow.core.model.DocumentMetadata;
import io.ycy.smartdocflow.core.model.DocumentResult;
import io.ycy.smartdocflow.core.spi.FormatExtractionResult;
import io.ycy.smartdocflow.core.spi.LayoutAnalyzer;
import io.ycy.smartdocflow.core.spi.LayoutResult;
import io.ycy.smartdocflow.core.spi.OcrResult;
import io.ycy.smartdocflow.core.model.DocumentProfile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BasicLayoutAnalyzer implements LayoutAnalyzer {
    private static final String COLUMN_SPLIT_REGEX = "\\s{2,}|\\t+|\\s\\|\\s|\\|";

    @Override
    public LayoutResult analyze(FormatExtractionResult extractionResult, OcrResult ocrResult, DocumentProfile profile) {
        List<DocumentBlock> blocks = buildBlocks(extractionResult, ocrResult, profile);

        DocumentMetadata metadata = new DocumentMetadata(
            UUID.randomUUID().toString(),
            extractionResult.source().getFileName().toString(),
            profile.sourceType(),
            extractionResult.pageCount(),
            "unknown"
        );

        return new LayoutResult(new DocumentResult(metadata, blocks, List.of()));
    }

    private List<DocumentBlock> buildBlocks(FormatExtractionResult extractionResult, OcrResult ocrResult, DocumentProfile profile) {
        if (ocrResult.applied() && profile.sourceType() == DocumentSourceType.IMAGE) {
            return buildBlocksFromLines(1, normalizeLines(ocrResult.text()), 0, 0.80d);
        }

        List<DocumentBlock> blocks = new ArrayList<>();
        int order = 0;
        List<String> pageTexts = extractionResult.pageTexts();
        List<List<String>> extractedPageLines = extractionResult.pageLines();
        HeaderFooterFilter filter = detectHeaderFooter(pageTexts);
        for (int i = 0; i < pageTexts.size(); i++) {
            String pageText = pageTexts.get(i);
            if (pageText == null || pageText.isBlank()) {
                continue;
            }
            List<String> pageLines = i < extractedPageLines.size() ? new ArrayList<>(extractedPageLines.get(i)) : normalizeLines(pageText);
            pageLines = stripHeaderFooter(pageLines, filter);
            List<DocumentBlock> pageBlocks = buildBlocksFromLines(i + 1, pageLines, order, ocrResult.applied() ? 0.80d : 0.95d);
            blocks.addAll(pageBlocks);
            order += pageBlocks.size();
        }

        if (blocks.isEmpty()) {
            blocks.addAll(buildBlocksFromLines(1, normalizeLines(extractionResult.extractedText()), 0, ocrResult.applied() ? 0.80d : 0.95d));
        }
        return blocks;
    }

    private List<DocumentBlock> buildBlocksFromLines(int page, List<String> pageLines, int startOrder, double confidence) {
        if (pageLines.isEmpty()) {
            return List.of();
        }

        List<DocumentBlock> blocks = new ArrayList<>();
        int order = startOrder;
        StringBuilder paragraph = new StringBuilder();
        List<String> tableCandidateLines = new ArrayList<>();

        for (String line : pageLines) {
            String normalizedLine = normalize(line);
            if (normalizedLine.isBlank()) {
                if (!tableCandidateLines.isEmpty()) {
                    blocks.add(singleBlock(page, order++, joinTableLines(tableCandidateLines), BlockType.TABLE, confidence));
                    tableCandidateLines.clear();
                }
                if (paragraph.length() > 0) {
                    String text = paragraph.toString().trim();
                    blocks.add(singleBlock(page, order++, text, detectBlockType(text), confidence));
                    paragraph.setLength(0);
                }
                continue;
            }

            if (isTableCandidate(normalizedLine)) {
                if (paragraph.length() > 0) {
                    String text = paragraph.toString().trim();
                    blocks.add(singleBlock(page, order++, text, detectBlockType(text), confidence));
                    paragraph.setLength(0);
                }
                tableCandidateLines.add(normalizedLine);
                continue;
            }

            if (!tableCandidateLines.isEmpty()) {
                blocks.add(singleBlock(page, order++, joinTableLines(tableCandidateLines), BlockType.TABLE, confidence));
                tableCandidateLines.clear();
            }

            if (isHeadingCandidate(normalizedLine)) {
                if (paragraph.length() > 0) {
                    String text = paragraph.toString().trim();
                    blocks.add(singleBlock(page, order++, text, detectBlockType(text), confidence));
                    paragraph.setLength(0);
                }
                blocks.add(singleBlock(page, order++, normalizedLine, BlockType.HEADING, confidence));
                continue;
            }

            if (paragraph.length() > 0) {
                paragraph.append(joinerBetween(paragraph.toString(), normalizedLine));
            }
            paragraph.append(normalizedLine.strip());
        }

        if (!tableCandidateLines.isEmpty()) {
            blocks.add(singleBlock(page, order++, joinTableLines(tableCandidateLines), BlockType.TABLE, confidence));
        }

        if (paragraph.length() > 0) {
            String text = paragraph.toString().trim();
            blocks.add(singleBlock(page, order, text, detectBlockType(text), confidence));
        }

        if (blocks.isEmpty() && !pageLines.isEmpty()) {
            String merged = String.join("\n", pageLines).trim();
            blocks.add(singleBlock(page, startOrder, merged, detectBlockType(merged), confidence));
        }
        return blocks;
    }

    private HeaderFooterFilter detectHeaderFooter(List<String> pageTexts) {
        Map<String, Integer> headerCounts = new HashMap<>();
        Map<String, Integer> footerCounts = new HashMap<>();

        for (String pageText : pageTexts) {
            List<String> lines = normalizeLines(pageText);
            if (lines.isEmpty()) {
                continue;
            }
            String header = firstNonBlank(lines);
            String footer = lastNonBlank(lines);
            if (!header.isBlank()) {
                headerCounts.merge(header, 1, Integer::sum);
            }
            if (!footer.isBlank()) {
                footerCounts.merge(footer, 1, Integer::sum);
            }
        }

        String repeatedHeader = findRepeatedLine(headerCounts);
        String repeatedFooter = findRepeatedLine(footerCounts);
        return new HeaderFooterFilter(repeatedHeader, repeatedFooter);
    }

    private List<String> stripHeaderFooter(List<String> lines, HeaderFooterFilter filter) {
        List<String> filtered = new ArrayList<>(lines);
        if (!filtered.isEmpty() && !filter.header().isBlank() && normalize(filtered.getFirst()).equals(filter.header())) {
            filtered.removeFirst();
        }
        if (!filtered.isEmpty() && !filter.footer().isBlank() && normalize(filtered.getLast()).equals(filter.footer())) {
            filtered.removeLast();
        }
        return filtered;
    }

    private String findRepeatedLine(Map<String, Integer> counts) {
        return counts.entrySet().stream()
            .filter(entry -> entry.getValue() >= 2)
            .filter(entry -> entry.getKey().length() <= 120)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse("");
    }

    private String firstNonBlank(List<String> lines) {
        return lines.stream().map(this::normalize).filter(line -> !line.isBlank()).findFirst().orElse("");
    }

    private String lastNonBlank(List<String> lines) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = normalize(lines.get(i));
            if (!line.isBlank()) {
                return line;
            }
        }
        return "";
    }

    private List<String> normalizeLines(String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return List.of();
        }
        return Arrays.stream(normalized.split("\\n"))
            .map(this::normalize)
            .toList()
            .stream()
            .toList();
    }

    private DocumentBlock singleBlock(int page, int order, String text, BlockType blockType, double confidence) {
        return new DocumentBlock(
            UUID.randomUUID().toString(),
            blockType,
            page,
            Bbox.EMPTY,
            order,
            text,
            confidence
        );
    }

    private BlockType detectBlockType(String text) {
        if (isHeadingCandidate(text)) {
            return BlockType.HEADING;
        }
        return BlockType.PARAGRAPH;
    }

    private boolean isHeadingCandidate(String text) {
        String singleLine = text.replace('\n', ' ').trim();
        if (singleLine.isBlank()) {
            return false;
        }
        if (singleLine.length() > 60) {
            return false;
        }
        if (singleLine.endsWith("。") || singleLine.endsWith(".") || singleLine.endsWith(";") || singleLine.endsWith("；") || singleLine.endsWith(":")) {
            return false;
        }
        long lineCount = Arrays.stream(text.split("\\n")).filter(line -> !line.isBlank()).count();
        if (lineCount > 2) {
            return false;
        }
        if (singleLine.contains("    ")) {
            return false;
        }
        return true;
    }

    private boolean isTableCandidate(String text) {
        String normalized = text.strip();
        if (normalized.isBlank()) {
            return false;
        }
        if (normalized.length() > 200) {
            return false;
        }
        String[] columns = normalized.split(COLUMN_SPLIT_REGEX);
        long nonBlankColumns = Arrays.stream(columns).map(String::strip).filter(value -> !value.isBlank()).count();
        if (nonBlankColumns < 3) {
            return false;
        }

        boolean hasNumericLikeCell = Arrays.stream(columns)
            .map(String::strip)
            .anyMatch(this::isNumericLike);

        return hasNumericLikeCell || normalized.contains("|") || normalized.contains("\t");
    }

    private boolean isNumericLike(String value) {
        return value.matches("[-+]?\\d[\\d,.:()%/-]*");
    }

    private String joinTableLines(List<String> lines) {
        return lines.stream().map(String::strip).filter(line -> !line.isBlank()).reduce((left, right) -> left + "\n" + right).orElse("");
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace("\r\n", "\n").trim();
    }

    private String joinerBetween(String previousText, String nextLine) {
        String prev = previousText.stripTrailing();
        String next = nextLine.stripLeading();
        if (prev.isEmpty() || next.isEmpty()) {
            return " ";
        }

        char prevLast = prev.charAt(prev.length() - 1);
        char nextFirst = next.charAt(0);

        if (prevLast == '-') {
            return "";
        }
        if (isSentenceEnding(prevLast)) {
            return "\n";
        }
        if (Character.isLowerCase(nextFirst) || Character.isDigit(nextFirst)) {
            return " ";
        }
        if (isCjk(nextFirst)) {
            return "";
        }
        return " ";
    }

    private boolean isSentenceEnding(char value) {
        return value == '.' || value == '!' || value == '?' || value == '。' || value == '！' || value == '？' || value == ':' || value == '：' || value == ';' || value == '；';
    }

    private boolean isCjk(char value) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(value);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
            || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS;
    }

    private record HeaderFooterFilter(String header, String footer) {
    }
}
