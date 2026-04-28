package io.ycy.smartdocflow.sdk;

import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.model.DocumentResult;
import io.ycy.smartdocflow.core.model.ParseOptions;
import io.ycy.smartdocflow.core.model.ir.Diagnostic;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class SmartDocFlow {
    private final DefaultSmartDocFlowEngine engine;

    public SmartDocFlow() {
        this.engine = new DefaultSmartDocFlowEngine();
    }

    public DocumentProfile profile(Path source) {
        validateSource(source);
        return engine.profile(source);
    }

    public DocumentResult parse(Path source) {
        validateSource(source);
        return engine.parse(source, ParseOptions.markdown());
    }

    public DocumentIr parseToIr(Path source) {
        validateSource(source);
        return engine.parseToIr(source);
    }

    public List<Diagnostic> parseDiagnostics(Path source) {
        return parseToIr(source).getDiagnostics();
    }

    public String parseToMarkdown(Path source) {
        validateSource(source);
        return engine.render(source, ParseOptions.markdown());
    }

    public String parseToJson(Path source) {
        validateSource(source);
        return engine.render(source, ParseOptions.json());
    }

    private static void validateSource(Path source) {
        if (source == null) {
            throw new IllegalArgumentException("输入文件不能为空");
        }
        if (!Files.exists(source)) {
            throw new IllegalArgumentException("输入文件不存在: " + source);
        }
        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException("输入路径不是文件: " + source);
        }
    }
}
