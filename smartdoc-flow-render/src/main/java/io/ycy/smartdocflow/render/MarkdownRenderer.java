package io.ycy.smartdocflow.render;

import io.ycy.smartdocflow.core.model.BlockType;
import io.ycy.smartdocflow.core.model.DocumentBlock;
import io.ycy.smartdocflow.core.model.DocumentResult;
import io.ycy.smartdocflow.core.spi.Renderer;

public final class MarkdownRenderer implements Renderer {
    @Override
    public String render(DocumentResult documentResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(documentResult.metadata().fileName()).append(System.lineSeparator()).append(System.lineSeparator());
        documentResult.blocks().forEach(block -> appendBlock(builder, block));
        return builder.toString().trim();
    }

    private void appendBlock(StringBuilder builder, DocumentBlock block) {
        if (block.type() == BlockType.HEADING || block.type() == BlockType.TITLE) {
            builder.append("## ").append(RenderSupport.normalizeInlineText(block.text()));
        } else if (block.type() == BlockType.TABLE) {
            builder.append("```text").append(System.lineSeparator());
            builder.append(RenderSupport.normalizeBlockText(block.text())).append(System.lineSeparator());
            builder.append("```");
        } else {
            builder.append(RenderSupport.normalizeBlockText(block.text()));
        }
        builder.append(System.lineSeparator()).append(System.lineSeparator());
    }
}
