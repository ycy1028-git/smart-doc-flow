package io.ycy.smartdocflow.core;

import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.model.DocumentResult;
import io.ycy.smartdocflow.core.model.ParseOptions;
import java.nio.file.Path;

public interface SmartDocFlowEngine {
    DocumentProfile profile(Path source);

    DocumentResult parse(Path source, ParseOptions options);

    String render(Path source, ParseOptions options);
}
