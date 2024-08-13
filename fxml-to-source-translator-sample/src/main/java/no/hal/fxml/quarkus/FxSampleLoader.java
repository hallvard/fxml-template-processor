package no.hal.fxml.quarkus;

import no.hal.fxml.quarkus.FxSampleControllerHelper;
import no.hal.fxml.runtime.FxLoaderContext;
import javafx.scene.control.Button;
import javafx.scene.Node;
import no.hal.fxml.quarkus.FxSampleController;
import javafx.scene.control.TextField;
import no.hal.fxml.runtime.AbstractFxLoader;
import javafx.scene.layout.AnchorPane;
import java.util.Map;
import no.hal.fxml.runtime.FxLoader;

// // generated from no/hal/fxml/quarkus/FxSample.fxml
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
      // <fx:define>
      // <String fx:id="strMessageFormat">
      java.lang.String string = java.lang.String.valueOf("Hello, %s!");
      this.setFxmlObject("strMessageFormat", string);
      // <TextField fx:id="txtName">
      TextField textField = new TextField();
      this.setFxmlObject("txtName", textField);
      textField.setId("txtName");
      textField.setOnAction(event -> this.controllerHelper.updateMessage1(event));
      textField.setPrefWidth(200.0);
      textField.setLayoutX(14.0);
      textField.setLayoutY(14.0);
      anchorPane.getChildren().add(textField);
      // <Button>
      Button button = new Button();
      button.setOnAction(event -> this.controllerHelper.updateMessage2(event));
      button.setLayoutX(226.0);
      button.setLayoutY(15.0);
      button.setText("Click!");
      button.setMnemonicParsing(false);
      anchorPane.getChildren().add(button);
      // <fx:include source="FxSampleIncluded.fxml"/>
      FxLoader<?, ?> fxIncludeLoader = fxLoaderContext.loadFxml("FxSampleIncluded.fxml");
      Node fxIncludeRoot = fxIncludeLoader.getRoot();
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
