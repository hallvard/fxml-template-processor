package no.hal.fxml.quarkus;

import java.io.IOException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkiverse.fx.PrimaryStage;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class FxSampleApplicationDelegate {

  public void start(@Observes @PrimaryStage Stage stage) throws IOException{
    Parent fxmlParent = new FxSampleLoader().load();
    Scene scene = new Scene(fxmlParent);
    stage.setScene(scene);
    stage.show();
  }
}