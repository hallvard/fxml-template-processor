package no.hal.fxml.runtime;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class DefaultFxLoaderProvider implements FxLoaderProvider {

    protected Map<Path, Supplier<FxLoader<?, ?>>> fxLoaderMap = new HashMap<>();

    public DefaultFxLoaderProvider(Map<Path, Supplier<FxLoader<?, ?>>> fxLoaderMap) {
        fxLoaderMap.putAll(fxLoaderMap);
    }

    @Override
    public boolean test(Path location) {
        return fxLoaderMap.containsKey(location);
    }

    @Override
    public FxLoader<?, ?> getFxLoader(Path location) {
        return fxLoaderMap.get(location).get();
    }
}
