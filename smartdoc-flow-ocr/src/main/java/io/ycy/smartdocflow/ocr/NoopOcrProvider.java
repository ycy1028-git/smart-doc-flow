package io.ycy.smartdocflow.ocr;

import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.model.ir.Diagnostic;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.spi.OcrProvider;
import java.nio.file.Path;

public final class NoopOcrProvider implements OcrProvider {
    private final String backendName;
    private final String reason;

    public NoopOcrProvider() {
        this("noop", "provider-disabled");
    }

    public NoopOcrProvider(String backendName, String reason) {
        this.backendName = backendName;
        this.reason = reason;
    }

    @Override
    public void process(Path source, DocumentIr ir, DocumentProfile profile) {
        ir.addDiagnostic(new Diagnostic("OCR", "backend", backendName, System.currentTimeMillis()));
        ir.addDiagnostic(new Diagnostic("OCR", "fallback", reason, System.currentTimeMillis()));
    }
}
