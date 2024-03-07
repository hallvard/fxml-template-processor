package no.hal.fxml.runtime;

import java.nio.file.Path;

import javafx.scene.Node;

public interface FxLoaderContext {

    FxLoader<?, ?> getFxLoader(Path location);

    default FxLoader<?, ?> loadFxml(Path location) {
        FxLoader<?, ?> fxLoader = getFxLoader(location);
        fxLoader.load(this);
        return fxLoader;
    }

    default FxLoader<?, ?> loadFxml(String location) {
        return loadFxml(Path.of(location));
    }

    default <N extends Node> N loadFxmlRoot(String location) {
        return (N) loadFxml(Path.of(location)).getRoot();
    }
}
