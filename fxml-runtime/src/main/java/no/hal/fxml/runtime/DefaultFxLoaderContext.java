package no.hal.fxml.runtime;

import java.nio.file.Path;
import java.util.List;

public class DefaultFxLoaderContext implements FxLoaderContext {

    private final Path basePath;
    private final List<FxLoaderProvider> fxLoaderProviders;

    public DefaultFxLoaderContext(Path basePath, FxLoaderProvider... fxLoaderProviders) {
        this.basePath = basePath;
        this.fxLoaderProviders = List.of(fxLoaderProviders);
    }

    @Override
    public FxLoader<?, ?> getFxLoader(Path location) {
        if (! location.isAbsolute()) {
            location = basePath.resolve(location);
        }
        for (var fxLoaderProvider : fxLoaderProviders) {
            var fxLoader = fxLoaderProvider.getFxLoader(location);
            if (fxLoader != null) {
                return fxLoader;
            }
        }
        throw new RuntimeException("No FxLoader for " + location);
    }
}
