package io.github.smartdocflow.render;

import io.github.smartdocflow.core.model.BlockType;
import io.github.smartdocflow.core.model.DocumentBlock;
import io.github.smartdocflow.core.model.DocumentResult;
import io.github.smartdocflow.core.spi.Renderer;

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
            builder.append("## ").append(block.text().replace('\n', ' ').trim());
        } else if (block.type() == BlockType.TABLE) {
            builder.append("```text").append(System.lineSeparator());
            builder.append(block.text().trim()).append(System.lineSeparator());
            builder.append("```");
        } else {
            builder.append(block.text().trim());
        }
        builder.append(System.lineSeparator()).append(System.lineSeparator());
    }
}
