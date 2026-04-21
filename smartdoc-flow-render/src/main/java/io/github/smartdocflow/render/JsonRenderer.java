package io.github.smartdocflow.render;

import io.github.smartdocflow.core.model.DocumentBlock;
import io.github.smartdocflow.core.model.DocumentResult;
import io.github.smartdocflow.core.spi.Renderer;
import java.util.stream.Collectors;

public final class JsonRenderer implements Renderer {
    @Override
    public String render(DocumentResult documentResult) {
        String blocks = documentResult.blocks().stream()
            .map(this::toJson)
            .collect(Collectors.joining(","));

        return "{" +
            "\"documentId\":\"" + escape(documentResult.metadata().documentId()) + "\"," +
            "\"fileName\":\"" + escape(documentResult.metadata().fileName()) + "\"," +
            "\"blocks\":[" + blocks + "]" +
            "}";
    }

    private String toJson(DocumentBlock block) {
        return "{" +
            "\"id\":\"" + escape(block.id()) + "\"," +
            "\"type\":\"" + block.type().name() + "\"," +
            "\"page\":" + block.page() + "," +
            "\"order\":" + block.order() + "," +
            "\"text\":\"" + escape(block.text()) + "\"" +
            "}";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
