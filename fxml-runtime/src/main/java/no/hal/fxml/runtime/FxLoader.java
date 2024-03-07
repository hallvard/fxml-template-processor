package no.hal.fxml.runtime;

import java.util.Map;

import javafx.scene.Node;

public interface FxLoader<N extends Node, C> {
            
    public N getRoot();

    public C getController();

    //

    public Map<String, Object> getNamespace();

    public default void setFxmlObject(String id, Object fxmlObject) {
        getNamespace().put(id, fxmlObject);
    }

    public default <T> T getFxmlObject(String id) {
        return (T) getNamespace().get(id);
    }

    public N load(FxLoaderContext fxLoaderContext);
}
