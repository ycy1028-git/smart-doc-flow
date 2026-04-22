package io.ycy.smartdocflow.core.pipeline;

import io.ycy.smartdocflow.core.model.BlockType;
import io.ycy.smartdocflow.core.model.DocumentAsset;
import io.ycy.smartdocflow.core.model.DocumentBlock;
import io.ycy.smartdocflow.core.model.DocumentMetadata;
import io.ycy.smartdocflow.core.model.DocumentResult;
import io.ycy.smartdocflow.core.model.ir.Asset;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.model.ir.DocumentMeta;
import io.ycy.smartdocflow.core.model.ir.Node;
import io.ycy.smartdocflow.core.model.ir.NodeType;
import java.util.ArrayList;
import java.util.List;

public final class IrResultMapper {

    private IrResultMapper() {
    }

    public static DocumentResult toDocumentResult(DocumentIr ir) {
        DocumentMeta meta = ir.getMeta();
        DocumentMetadata metadata = new DocumentMetadata(
            meta.documentId(),
            meta.sourceName(),
            meta.sourceType(),
            meta.pageCount(),
            meta.languageHints().isEmpty() ? null : meta.languageHints().getFirst()
        );

        List<DocumentBlock> blocks = new ArrayList<>();
        int order = 0;
        for (Node node : ir.getNodes()) {
            BlockType blockType = mapNodeType(node.nodeType());
            if (blockType != null) {
                blocks.add(new DocumentBlock(
                    node.id(),
                    blockType,
                    resolvePage(ir, node.containerId()),
                    node.bbox(),
                    order++,
                    node.text(),
                    node.confidence()
                ));
            }
        }

        List<DocumentAsset> assets = new ArrayList<>();
        for (Asset asset : ir.getAssets()) {
            assets.add(new DocumentAsset(asset.id(), asset.assetType(), asset.ref()));
        }

        return new DocumentResult(metadata, blocks, assets);
    }

    private static BlockType mapNodeType(NodeType nodeType) {
        return switch (nodeType) {
            case TITLE -> BlockType.TITLE;
            case HEADING -> BlockType.HEADING;
            case PARAGRAPH -> BlockType.PARAGRAPH;
            case LIST, LIST_ITEM -> BlockType.LIST;
            case TABLE, TABLE_ROW, TABLE_CELL -> BlockType.TABLE;
            case FIGURE -> BlockType.FIGURE;
            case CAPTION -> BlockType.CAPTION;
            case FORMULA -> BlockType.FORMULA;
            case CODE_BLOCK -> BlockType.CODE_BLOCK;
            case FOOTNOTE -> BlockType.FOOTNOTE;
            case NOTE -> BlockType.FOOTNOTE;
            case HEADER, FOOTER, PAGE_NUMBER -> null;
            case QUOTE, COMMENT -> BlockType.PARAGRAPH;
        };
    }

    private static int resolvePage(DocumentIr ir, String containerId) {
        if (containerId == null) {
            return 0;
        }
        return ir.getContainers().stream()
            .filter(c -> c.id().equals(containerId))
            .mapToInt(c -> c.index())
            .findFirst()
            .orElse(0);
    }
}
