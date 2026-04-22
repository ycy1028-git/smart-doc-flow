package io.ycy.smartdocflow.core.model;

public record ParseOptions(OutputFormat outputFormat) {
    public static ParseOptions markdown() {
        return new ParseOptions(OutputFormat.MARKDOWN);
    }

    public static ParseOptions json() {
        return new ParseOptions(OutputFormat.JSON);
    }
}
