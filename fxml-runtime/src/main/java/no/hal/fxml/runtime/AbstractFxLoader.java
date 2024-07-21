package no.hal.fxml.runtime;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.Node;

public abstract class AbstractFxLoader<N extends Node, C> implements FxLoader<N, C> {
            
    private Map<String, Object> namespace = new HashMap<>();
    
    public AbstractFxLoader() {
    }

    public AbstractFxLoader(Map<String, Object> mappings) {
        namespace.putAll(mappings);
    }

    private N root = null;

    // @Override public if FXML contains <fx:root> element
    protected void setRoot(N root) {
        this.root = root;
    }

    @Override
    public N getRoot() {
        return root;
    }

    protected C controller;

    @Override
    public C getController() {
        return controller;
    }

    //

    @Override
    public Map<String, Object> getNamespace() {
        return namespace;
    }

    @Override
    public N load(FxLoaderContext fxLoaderContext) {
        N node = build(fxLoaderContext);
        if (this.root == null) {
            this.root = node;
        }
        this.controller = createController();
        if (this.controller != null) {
            initializeController();
        }
        return node;
    }

    protected abstract N build(FxLoaderContext fxLoaderContext);

    protected C createController() {
        return null;
    }

    protected void initializeController() {
    }
}
