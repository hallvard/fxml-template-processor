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

  @Inject
  FxmlTemplateProcessorProvider FXML;

  public void start(@Observes @PrimaryStage Stage stage) throws IOException{
    Parent fxmlParent = (Parent) FXML.get(this)."""
    <AnchorPane xmlns:fx="http://javafx.com/fxml" id="AnchorPane" prefHeight="188.0" prefWidth="406.0">
        <TextField layoutX="14.0" layoutY="14.0" prefWidth="200.0" fx:id="txtName"/>
        <Button layoutX="226.0" layoutY="15.0" mnemonicParsing="false" onAction="#updateMessage" text="Click!"/>
        <Label fx:id="lblMessage" layoutX="14.0" layoutY="44.0" prefHeight="21.0" prefWidth="264.0" text="Type your name and click!">
        </Label>
    </AnchorPane>
    """;

    Scene scene = new Scene(fxmlParent);
    stage.setScene(scene);
    stage.show();
  }

    @FXML
    Label lblMessage;

    @FXML
    TextField txtName;

    @ConfigProperty(name="app.greeting.format")
    String greetingFormat;

    public void updateMessage() {
        lblMessage.setText(greetingFormat.formatted(txtName.getText()));
    }
}