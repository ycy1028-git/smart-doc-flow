package io.ycy.smartdocflow.render;

import io.ycy.smartdocflow.core.model.BlockType;
import io.ycy.smartdocflow.core.model.DocumentBlock;
import io.ycy.smartdocflow.core.model.DocumentResult;
import io.ycy.smartdocflow.core.model.OutputFormat;
import io.ycy.smartdocflow.core.model.ParseOptions;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.pipeline.IrResultMapper;
import io.ycy.smartdocflow.core.spi.ResultRenderer;

public final class IrMarkdownRenderer implements ResultRenderer {
    @Override
    public String render(DocumentIr ir, ParseOptions options) {
        DocumentResult result = IrResultMapper.toDocumentResult(ir);

        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(result.metadata().fileName()).append(System.lineSeparator()).append(System.lineSeparator());
        result.blocks().forEach(block -> appendBlock(builder, block));
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
