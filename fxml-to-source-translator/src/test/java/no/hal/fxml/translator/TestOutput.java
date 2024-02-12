package no.hal.fxml.translator;

import java.util.Map;
import no.hal.fxml.builder.AbstractFxBuilder;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.lang.String;
import javafx.scene.layout.Pane;


public class TestOutput extends AbstractFxBuilder<Pane, Object> {

    public TestOutput() {
        super();
    }
    public TestOutput(Map<String, Object> namespace) {
        super(namespace);
    }

    public Pane build() {
        Pane pane = new Pane();
        String string = String.valueOf("Enter answer");
        setFxmlObject("prompt", string);
        TextField textField = new TextField();
        setFxmlObject("answerInput", textField);
        textField.setId("answerInput");
        textField.setPromptText(getFxmlObject("prompt"));
        Label label = new Label();
        setFxmlObject("label1", label);
        label.setId("label1");
        label.setText("Hi!");
        pane.getChildren().add(label);
        pane.getChildren().add(getFxmlObject("answerInput"));
        return pane;

    }
}