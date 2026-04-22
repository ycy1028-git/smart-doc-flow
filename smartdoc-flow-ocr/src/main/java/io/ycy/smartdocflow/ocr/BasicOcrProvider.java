package io.ycy.smartdocflow.ocr;

import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.spi.OcrProvider;

public final class BasicOcrProvider implements OcrProvider {
    @Override
    public void process(DocumentIr ir, DocumentProfile profile) {
        if (!profile.scanned()) {
            return;
        }

        ir.addNode(new io.ycy.smartdocflow.core.model.ir.Node(
            java.util.UUID.randomUUID().toString(),
            io.ycy.smartdocflow.core.model.ir.NodeType.PARAGRAPH,
            null,
            io.ycy.smartdocflow.common.model.Bbox.EMPTY,
            "[OCR placeholder for scanned document]",
            java.util.List.of(),
            0.5,
            "0",
            java.util.List.of(),
            java.util.Map.of(),
            java.util.Set.of("OCR")
        ));
    }
}
