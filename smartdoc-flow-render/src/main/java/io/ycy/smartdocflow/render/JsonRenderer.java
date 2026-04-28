package io.ycy.smartdocflow.render;

import io.ycy.smartdocflow.core.model.DocumentBlock;
import io.ycy.smartdocflow.core.model.DocumentResult;
import io.ycy.smartdocflow.core.spi.Renderer;
import java.util.stream.Collectors;

public final class JsonRenderer implements Renderer {
    @Override
    public String render(DocumentResult documentResult) {
        String blocks = documentResult.blocks().stream()
            .map(this::toJson)
            .collect(Collectors.joining(","));

        return "{" +
            "\"documentId\":\"" + RenderSupport.escapeJson(documentResult.metadata().documentId()) + "\"," +
            "\"fileName\":\"" + RenderSupport.escapeJson(documentResult.metadata().fileName()) + "\"," +
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
