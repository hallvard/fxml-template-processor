package no.hal.fxml.builder;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.Node;

public abstract class AbstractFxBuilder<N extends Node, C> {
            
    private Map<String, Object> namespace = new HashMap<>();
    
    public AbstractFxBuilder() {
    }

    public AbstractFxBuilder(Map<String, Object> mappings) {
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

    // @Override public if FXML doesn't contain fx:controller attribute
    protected void setController(C controller) {
        this.controller = controller;
    }

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

    protected abstract N build();
}
