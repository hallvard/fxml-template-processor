package no.hal.fxml.runtime;

import java.util.Map;

public abstract class AbstractFxControllerHelper<C> {
            
    private final Map<String, Object> namespace;
    protected final C controller;
    
    protected AbstractFxControllerHelper(Map<String, Object> namespace, C controller) {
        this.namespace = namespace;
        this.controller = controller;
    }

    //

    public <T> T getFxmlObject(String id) {
        return (T) namespace.get(id);
    }

    public abstract void initializeController();
}
