package no.hal.fxml.translator;

import java.util.Map;
import no.hal.fxml.builder.AbstractFxBuilder;

import javafx.scene.paint.Color;
import javafx.scene.control.Label;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.TextField;
import java.lang.String;
import javafx.scene.layout.Pane;

public class TestOutput extends AbstractFxBuilder<Pane, FxmlTranslatorTest.Controller> {

   public TestOutput() {
      super();
   }

   public TestOutput(Map<String, Object> namespace) {
      super(namespace);
   }

   protected Pane load() {
    var root = build();
    // only if controller is declared in FXML
    setController(new FxmlTranslatorTest.Controller());
    initializeController();
    return root;
   }

   protected Pane build() {
      Pane pane = new Pane();
      String string = String.valueOf("Enter answer");
      setFxmlObject("prompt", string);
      TextField textField = new TextField();
      setFxmlObject("answerInput", textField);
      textField.setId("answerInput");
      textField.setOnAction(event -> this.controller.onAnswerInput(event));
      textField.setPromptText(getFxmlObject("prompt"));
      Color color = new Color(1.0, 0.0, 0.0, 1);
      setFxmlObject("red", color);
      Label label = new Label();
      setFxmlObject("label1", label);
      label.setId("label1");
      label.setText("Hi!");
      pane.getChildren().add(label);
      pane.getChildren().add(getFxmlObject("answerInput"));
      Rectangle rectangle = new Rectangle();
      rectangle.setFill(getFxmlObject("red"));
      pane.getChildren().add(rectangle);
      return pane;
   }

   protected void initializeController() {
      this.controller.red = getFxmlObject("red");
      this.controller.setLabel1(getFxmlObject("label1"));
   }
}