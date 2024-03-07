package no.hal.fxml.runtime;

import java.nio.file.Path;
import java.util.function.Predicate;

public interface FxLoaderProvider extends Predicate<Path> {
    FxLoader<?, ?> getFxLoader(Path location);
}
