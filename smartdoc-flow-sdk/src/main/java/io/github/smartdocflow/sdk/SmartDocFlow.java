package io.github.smartdocflow.sdk;

import io.github.smartdocflow.core.model.DocumentProfile;
import io.github.smartdocflow.core.model.DocumentResult;
import io.github.smartdocflow.core.model.ParseOptions;
import java.nio.file.Path;

public final class SmartDocFlow {
    private final DefaultSmartDocFlowEngine engine;

    public SmartDocFlow() {
        this.engine = new DefaultSmartDocFlowEngine();
    }

    public DocumentProfile profile(Path source) {
        return engine.profile(source);
    }

    public DocumentResult parse(Path source) {
        return engine.parse(source, ParseOptions.markdown());
    }

    public String parseToMarkdown(Path source) {
        return engine.render(source, ParseOptions.markdown());
    }

    public String parseToJson(Path source) {
        return engine.render(source, ParseOptions.json());
    }
}
