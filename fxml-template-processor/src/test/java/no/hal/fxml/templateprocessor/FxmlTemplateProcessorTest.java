package no.hal.fxml.templateprocessor;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import static no.hal.fxml.templateprocessor.FxmlTemplateProcessor.FXML;

@ExtendWith(ApplicationExtension.class)
public class FxmlTemplateProcessorTest {

    private Pane root;
    private Label label3;

    @Start
    public void start(Stage stage) {
        // attribute value
        var labelText = "Hi 1";
        var label1 = FXML()."""
        <Label fx:id="label1" text="\{labelText}"/>
        """;

        // attribute
        var labelAttribute = Map.of("text", "Hi 2");
        var label2 = FXML()."""
        <Label fx:id="label2" \{labelAttribute}/>
        """;

        // elememnt
        this.label3 = new Label("Hi 3");
        this.label3.setId("label3");
        var label3Pane = FXML()."""
        \{label3}
        """;

        this.root = new Pane(label1, label2, label3Pane);
        stage.setScene(new Scene(this.root));
        stage.show();
    }

    @Test
    public void test(FxRobot robot) {
        FxAssert.verifyThat("#label1", LabeledMatchers.hasText("Hi 1"));
        FxAssert.verifyThat("#label2", LabeledMatchers.hasText("Hi 2"));
        FxAssert.verifyThat("#label3", LabeledMatchers.hasText("Hi 3"));
        Assertions.assertSame(this.label3, robot.lookup("#label3").query());
    }
}
