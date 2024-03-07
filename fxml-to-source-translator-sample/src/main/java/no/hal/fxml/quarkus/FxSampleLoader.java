package no.hal.fxml.quarkus;

import no.hal.fxml.quarkus.FxSampleControllerHelper;
import no.hal.fxml.runtime.FxLoaderContext;
import javafx.scene.control.Button;
import no.hal.fxml.quarkus.FxSampleController;
import javafx.scene.control.TextField;
import no.hal.fxml.runtime.AbstractFxLoader;
import javafx.scene.layout.AnchorPane;
import java.util.Map;

public class FxSampleLoader extends AbstractFxLoader<AnchorPane, FxSampleController> {

   public FxSampleLoader() {
      super();
   }

   public FxSampleLoader(Map<String, Object> namespace) {
      super(namespace);
   }

   protected AnchorPane build(FxLoaderContext fxLoaderContext) {
      // <AnchorPane>
      AnchorPane anchorPane = new AnchorPane();
      anchorPane.setPrefHeight(188.0);
      anchorPane.setPrefWidth(406.0);
      anchorPane.setId("AnchorPane");
      // <TextField fx:id="txtName">
      TextField textField = new TextField();
      this.setFxmlObject("txtName", textField);
      textField.setId("txtName");
      textField.setOnAction(this.controllerHelper::updateMessage1);
      textField.setPrefWidth(200.0);
      textField.setLayoutX(14.0);
      textField.setLayoutY(14.0);
      anchorPane.getChildren().add(textField);
      // <Button>
      Button button = new Button();
      button.setOnAction(this.controllerHelper::updateMessage2);
      button.setLayoutX(226.0);
      button.setLayoutY(15.0);
      button.setText("Click!");
      button.setMnemonicParsing(false);
      anchorPane.getChildren().add(button);
      // <fx:include source="FxSampleIncluded.fxml"/>
      var fxIncludeLoader = fxLoaderContext.loadFxml("FxSampleIncluded.fxml");
      var fxIncludeRoot = fxIncludeLoader.getRoot();
      this.setFxmlObject("lblMessage", fxIncludeRoot);
      this.setFxmlObject("lblMessageController", fxIncludeLoader.getController());
      anchorPane.getChildren().add(fxIncludeRoot);
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
