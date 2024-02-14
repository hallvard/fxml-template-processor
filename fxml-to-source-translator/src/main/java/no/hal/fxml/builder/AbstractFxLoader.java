package no.hal.fxml.builder;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.Node;

public abstract class AbstractFxLoader<N extends Node, C> {
            
    private Map<String, Object> namespace = new HashMap<>();
    
    public AbstractFxLoader() {
    }

    public AbstractFxLoader(Map<String, Object> mappings) {
        namespace.putAll(mappings);
    }

    private N root;

    // @Override public if FXML contains <fx:root> element
    protected void setRoot(N root) {
        this.root = root;
    }

    public N getRoot() {
        return root;
    }

    protected C controller;

    public C getController() {
        return controller;
    }

    //

    public Map<String, Object> getNamespace() {
        return namespace;
    }

    public void setFxmlObject(String id, Object fxmlObject) {
        getNamespace().put(id, fxmlObject);
    }

    public <T> T getFxmlObject(String id) {
        return (T) getNamespace().get(id);
    }

    public N load() {
        N node = build();
        this.controller = createController();
        if (this.controller != null) {
            initializeController();
        }
        return node;
    }

    protected abstract N build();

    protected C createController() {
        return null;
    }

    protected void initializeController() {
    }
}
