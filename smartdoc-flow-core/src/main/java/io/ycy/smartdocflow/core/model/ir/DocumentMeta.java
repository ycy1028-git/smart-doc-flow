package io.ycy.smartdocflow.core.model.ir;

import io.ycy.smartdocflow.common.model.DocumentSourceType;
import java.util.List;

public record DocumentMeta(
    String documentId,
    DocumentSourceType sourceType,
    String sourceName,
    int pageCount,
    List<String> languageHints,
    boolean scanned,
    boolean tableHeavy,
    boolean imageHeavy,
    boolean multiColumn,
    String generator,
    String parseVersion
) {
}
