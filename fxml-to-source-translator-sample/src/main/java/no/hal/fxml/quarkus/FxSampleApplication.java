package no.hal.fxml.quarkus;

import io.quarkiverse.fx.FxApplication;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import javafx.application.Application;

@QuarkusMain
public class FxSampleApplication implements QuarkusApplication {

  public static void main(String[] args) {
    Quarkus.run(FxSampleApplication.class);
  }

  @Override
  public int run(String... args) {
    Application.launch(FxApplication.class, args);
    return 0;
  }
}