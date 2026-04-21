package io.github.smartdocflow.core;

import io.github.smartdocflow.core.model.DocumentProfile;
import io.github.smartdocflow.core.model.DocumentResult;
import io.github.smartdocflow.core.model.ParseOptions;
import java.nio.file.Path;

public interface SmartDocFlowEngine {
    DocumentProfile profile(Path source);

    DocumentResult parse(Path source, ParseOptions options);

    String render(Path source, ParseOptions options);
}
