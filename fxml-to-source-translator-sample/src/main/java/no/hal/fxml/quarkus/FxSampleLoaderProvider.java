package no.hal.fxml.quarkus;

import java.nio.file.Path;
import no.hal.fxml.quarkus.FxSampleIncludedLoader;
import no.hal.fxml.runtime.DefaultFxLoaderProvider;
import no.hal.fxml.quarkus.FxSampleLoader;
import java.util.Map;

// // generated
public class FxSampleLoaderProvider extends DefaultFxLoaderProvider {

   public FxSampleLoaderProvider() {
      super(Map.of(Path.of("/no/hal/fxml/quarkus/FxSample.fxml"), () -> new FxSampleLoader(), Path.of("/no/hal/fxml/quarkus/FxSampleIncluded.fxml"), () -> new FxSampleIncludedLoader()));
   }
}
