package io.ycy.smartdocflow.render;

import io.ycy.smartdocflow.core.model.DocumentBlock;
import io.ycy.smartdocflow.core.model.DocumentResult;
import io.ycy.smartdocflow.core.model.ParseOptions;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.pipeline.IrResultMapper;
import io.ycy.smartdocflow.core.spi.ResultRenderer;
import java.util.stream.Collectors;

public final class IrJsonRenderer implements ResultRenderer {
    @Override
    public String render(DocumentIr ir, ParseOptions options) {
        DocumentResult result = IrResultMapper.toDocumentResult(ir);

        String blocks = result.blocks().stream()
            .map(this::toJson)
            .collect(Collectors.joining(","));

        return "{" +
            "\"documentId\":\"" + RenderSupport.escapeJson(result.metadata().documentId()) + "\"," +
            "\"fileName\":\"" + RenderSupport.escapeJson(result.metadata().fileName()) + "\"," +
            "\"blocks\":[" + blocks + "]" +
            "}";
    }

    private String toJson(DocumentBlock block) {
        return "{" +
            "\"id\":\"" + RenderSupport.escapeJson(block.id()) + "\"," +
            "\"type\":\"" + block.type().name() + "\"," +
            "\"page\":" + block.page() + "," +
            "\"order\":" + block.order() + "," +
            "\"text\":\"" + RenderSupport.escapeJson(block.text()) + "\"" +
            "}";
    }
}
