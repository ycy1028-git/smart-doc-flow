package io.github.smartdocflow.common.model;

public record Bbox(double x, double y, double width, double height) {
    public static final Bbox EMPTY = new Bbox(0, 0, 0, 0);
}
