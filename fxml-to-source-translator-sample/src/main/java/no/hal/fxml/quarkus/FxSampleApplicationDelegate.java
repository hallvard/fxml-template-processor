package no.hal.fxml.quarkus;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import io.quarkiverse.fx.FxPostStartupEvent;
import jakarta.enterprise.event.Observes;
import javafx.scene.Parent;
import javafx.scene.Scene;
import no.hal.fxml.runtime.DefaultFxLoaderContext;
import no.hal.fxml.runtime.DefaultFxLoaderProvider;
import no.hal.fxml.runtime.FxLoaderContext;

public class FxSampleApplicationDelegate {

  public void start(@Observes FxPostStartupEvent fxStartupEvent) throws IOException {
    FxLoaderContext fxLoaderContext = new DefaultFxLoaderContext(
      Path.of("/no/hal/fxml/quarkus/"),
      new FxSampleLoaderProvider()
      // new DefaultFxLoaderProvider(Map.of(
      //   Path.of("/no/hal/fxml/quarkus/FxSample.fxml"), () -> new FxSampleLoader(),
      //   Path.of("/no/hal/fxml/quarkus/FxSampleIncluded.fxml"), () -> new FxSampleIncludedLoader()
      // ))
    );
    Parent fxmlParent = fxLoaderContext.loadFxmlRoot("FxSample.fxml");
    Scene scene = new Scene(fxmlParent);
    fxStartupEvent.getPrimaryStage().setScene(scene);
    fxStartupEvent.getPrimaryStage().show();
  }
}