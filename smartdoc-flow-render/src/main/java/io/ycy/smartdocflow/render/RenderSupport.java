package io.ycy.smartdocflow.render;

final class RenderSupport {
    private RenderSupport() {
    }

    static String normalizeInlineText(String value) {
        return normalizeBlockText(value).replace('\n', ' ');
    }

    static String normalizeBlockText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    static String escapeJson(String value) {
        return normalizeNull(value)
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    static String normalizeNull(String value) {
        return value == null ? "" : value;
    }
}
