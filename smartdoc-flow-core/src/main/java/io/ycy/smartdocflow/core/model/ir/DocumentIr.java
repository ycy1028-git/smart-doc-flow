package io.ycy.smartdocflow.core.model.ir;

import java.util.ArrayList;
import java.util.List;

public class DocumentIr {

    private DocumentMeta meta;
    private final List<Container> containers;
    private final List<Node> nodes;
    private final List<Relation> relations;
    private final List<Asset> assets;
    private final List<Diagnostic> diagnostics;

    public DocumentIr() {
        this.containers = new ArrayList<>();
        this.nodes = new ArrayList<>();
        this.relations = new ArrayList<>();
        this.assets = new ArrayList<>();
        this.diagnostics = new ArrayList<>();
    }

    public DocumentIr(DocumentMeta meta) {
        this();
        this.meta = meta;
    }

    public DocumentMeta getMeta() {
        return meta;
    }

    public void setMeta(DocumentMeta meta) {
        this.meta = meta;
    }

    public List<Container> getContainers() {
        return containers;
    }

    public void addContainer(Container container) {
        containers.add(container);
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void addNode(Node node) {
        nodes.add(node);
    }

    public List<Relation> getRelations() {
        return relations;
    }

    public void addRelation(Relation relation) {
        relations.add(relation);
    }

    public List<Asset> getAssets() {
        return assets;
    }

    public void addAsset(Asset asset) {
        assets.add(asset);
    }

    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

    public void addDiagnostic(Diagnostic diagnostic) {
        diagnostics.add(diagnostic);
    }
}
