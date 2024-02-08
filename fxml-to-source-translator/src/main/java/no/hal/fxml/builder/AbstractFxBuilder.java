package no.hal.fxml.builder;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.Node;

public abstract class AbstractFxBuilder<N extends Node> {
            
    private Map<String, Object> namespace = new HashMap<>();
    
    public AbstractFxBuilder() {
    }

    public AbstractFxBuilder(Map<String, Object> mappings) {
        namespace.putAll(mappings);
    }

    public Map<String, Object> getNamespace() {
        return namespace;
    }

    public void setFxmlObject(String id, Object fxmlObject) {
        getNamespace().put(id, fxmlObject);
    }

    public <T> T getFxmlObject(String id) {
        return (T) getNamespace().get(id);
    }

    public abstract N build();
}
