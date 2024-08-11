package no.hal.fxml.quarkus;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class FxSampleController {
    
    @FXML
    Label lblMessage;

    private TextField nameTextField;

    @FXML
    void setTxtName(TextField tf) {
        nameTextField = tf;
    }

    @FXML
    String strMessageFormat;

    @FXML
    void initialize() {
    }

    @FXML
    void updateMessage1() {
        lblMessage.setText(strMessageFormat.formatted(nameTextField.getText()));
    }

    @FXML
    void updateMessage2(ActionEvent actionEvent) {
        updateMessage1();
    }
}
