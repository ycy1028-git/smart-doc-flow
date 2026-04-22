package io.ycy.smartdocflow.core.spi;

import io.ycy.smartdocflow.core.model.DocumentProfile;
import java.nio.file.Path;

public interface DocumentProfiler {
    DocumentProfile profile(Path source);
}
