package no.hal.fxml.quarkus;

import no.hal.fxml.quarkus.FxSampleControllerHelper;
import javafx.scene.control.Button;
import no.hal.fxml.quarkus.FxSampleController;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import no.hal.fxml.runtime.AbstractFxLoader;
import javafx.scene.layout.Pane;
import java.util.Map;
import javafx.scene.layout.AnchorPane;

public class FxSampleLoader extends AbstractFxLoader<Pane, FxSampleController> {

   public FxSampleLoader() {
      super();
   }

   public FxSampleLoader(Map<String, Object> namespace) {
      super(namespace);
   }

   protected AnchorPane build() {
      AnchorPane anchorPane = new AnchorPane();
      anchorPane.setPrefHeight(188.0);
      anchorPane.setPrefWidth(406.0);
      anchorPane.setId("AnchorPane");
      TextField textField = new TextField();
      this.setFxmlObject("txtName", textField);
      textField.setId("txtName");
      textField.setOnAction(this.controllerHelper::updateMessage1);
      textField.setPrefWidth(200.0);
      textField.setLayoutX(14.0);
      textField.setLayoutY(14.0);
      anchorPane.getChildren().add(textField);
      Button button = new Button();
      button.setOnAction(this.controllerHelper::updateMessage2);
      button.setLayoutX(226.0);
      button.setLayoutY(15.0);
      button.setText("Click!");
      button.setMnemonicParsing(false);
      anchorPane.getChildren().add(button);
      Label label = new Label();
      this.setFxmlObject("lblMessage", label);
      label.setId("lblMessage");
      label.setPrefHeight(21.0);
      label.setPrefWidth(264.0);
      label.setLayoutX(14.0);
      label.setLayoutY(44.0);
      label.setText("Type your name and click!");
      anchorPane.getChildren().add(label);
      return anchorPane;
   }

   protected FxSampleController createController() {
      return new FxSampleController();
   }

   private FxSampleControllerHelper controllerHelper;

   protected void initializeController() {
      this.controllerHelper = new FxSampleControllerHelper(this.getNamespace(), this.controller);
      this.controllerHelper.initializeController();
   }
}
