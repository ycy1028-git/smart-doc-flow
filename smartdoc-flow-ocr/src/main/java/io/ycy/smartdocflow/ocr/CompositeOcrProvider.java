package io.ycy.smartdocflow.ocr;

import io.ycy.smartdocflow.common.model.DocumentSourceType;
import io.ycy.smartdocflow.core.model.DocumentProfile;
import io.ycy.smartdocflow.core.model.ir.Diagnostic;
import io.ycy.smartdocflow.core.model.ir.DocumentIr;
import io.ycy.smartdocflow.core.spi.OcrProvider;
import java.nio.file.Path;
import java.util.List;

public final class CompositeOcrProvider implements OcrProvider {
    private static final String DEFAULT_PROVIDER = "auto";
    private final List<OcrRoute> routes;
    private final OcrProvider fallbackProvider;

    public CompositeOcrProvider(List<OcrRoute> routes, OcrProvider fallbackProvider) {
        this.routes = routes;
        this.fallbackProvider = fallbackProvider;
    }

    @Override
    public void process(Path source, DocumentIr ir, DocumentProfile profile) {
        for (OcrRoute route : routes) {
            if (route.supports(profile)) {
                ir.addDiagnostic(new Diagnostic("OCR", "route", route.name(), System.currentTimeMillis()));
                ir.addDiagnostic(new Diagnostic("OCR", "provider", route.providerName(), System.currentTimeMillis()));
                ir.addDiagnostic(new Diagnostic("OCR", "costTier", route.costTier(), System.currentTimeMillis()));
                ir.addDiagnostic(new Diagnostic("OCR", "qualityHint", route.qualityHint(), System.currentTimeMillis()));
                route.provider().process(source, ir, profile);
                return;
            }
        }

        ir.addDiagnostic(new Diagnostic("OCR", "route", "fallback", System.currentTimeMillis()));
        ir.addDiagnostic(new Diagnostic("OCR", "provider", "fallback", System.currentTimeMillis()));
        ir.addDiagnostic(new Diagnostic("OCR", "costTier", "standard", System.currentTimeMillis()));
        ir.addDiagnostic(new Diagnostic("OCR", "qualityHint", "fallback", System.currentTimeMillis()));
        fallbackProvider.process(source, ir, profile);
    }

    public static CompositeOcrProvider basic() {
        return basic(resolvePreferredProvider());
    }

    static CompositeOcrProvider basic(String preferredProvider) {
        OcrProvider selected = createProvider(preferredProvider);
        String providerName = normalizeProviderName(preferredProvider);
        OcrProvider fallback = new NoopOcrProvider("noop", "no-route-matched");
        return new CompositeOcrProvider(
            List.of(
                new OcrRoute("image-high-detail", providerName, profile -> profile.sourceType() == DocumentSourceType.IMAGE && profile.imageHeavy(), selected),
                new OcrRoute("image-basic", providerName, profile -> profile.sourceType() == DocumentSourceType.IMAGE, selected),
                new OcrRoute("scanned-pdf-high-detail", providerName, profile -> profile.sourceType() == DocumentSourceType.PDF && profile.scanned() && (profile.imageHeavy() || profile.multiColumn()), selected),
                new OcrRoute("scanned-pdf-basic", providerName, profile -> profile.sourceType() == DocumentSourceType.PDF && profile.scanned(), selected)
            ),
            fallback
        );
    }

    private static OcrProvider createProvider(String preferredProvider) {
        return switch (normalizeProviderName(preferredProvider)) {
            case "noop" -> new NoopOcrProvider();
            case "tesseract", "auto" -> new BasicOcrProvider();
            default -> new NoopOcrProvider("noop", "unknown-provider");
        };
    }

    private static String resolvePreferredProvider() {
        String configured = System.getenv("SMARTDOC_FLOW_OCR_PROVIDER");
        if (configured == null || configured.isBlank()) {
            return DEFAULT_PROVIDER;
        }
        return configured.trim();
    }

    private static String normalizeProviderName(String preferredProvider) {
        if (preferredProvider == null || preferredProvider.isBlank()) {
            return DEFAULT_PROVIDER;
        }
        return preferredProvider.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public record OcrRoute(String name, String providerName, java.util.function.Predicate<DocumentProfile> predicate, OcrProvider provider) {
        boolean supports(DocumentProfile profile) {
            return predicate.test(profile);
        }

        String costTier() {
            return name.contains("high-detail") ? "high" : "standard";
        }

        String qualityHint() {
            return name.contains("high-detail") ? "detail-priority" : "default-priority";
        }
    }
}
