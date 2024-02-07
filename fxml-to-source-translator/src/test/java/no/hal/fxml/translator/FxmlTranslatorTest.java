package no.hal.fxml.translator;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import no.hal.fxml.model.FxmlCode.Document;
import no.hal.fxml.model.JavaCode.Literal;
import no.hal.fxml.model.JavaCode.MethodCall;
import no.hal.fxml.model.JavaCode.Statement;
import no.hal.fxml.model.JavaCode.VariableDeclaration;
import no.hal.fxml.model.JavaCode.VariableExpression;
import no.hal.fxml.parser.FxmlParser;

public class FxmlTranslatorTest {

    private void testFxmlTranslator(Document fxmlDoc, List<Statement> expected) throws Exception {
        List<Statement> actual = null;
        try {
            actual = FxmlTranslator.translateFxml(fxmlDoc, getClass().getClassLoader());
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        Assertions.assertEquals(expected, actual);
    }

    private void testFxmlTranslator(String fxmlSource, List<Statement> expected) throws Exception {
        testFxmlTranslator(FxmlParser.parseFxml(fxmlSource), expected);
    }

    @Test
    public void testFxmlTranslator() throws Exception {
        testFxmlTranslator("""
            <?import javafx.scene.control.*?>
            <?import javafx.scene.layout.*?>
            <Pane xmlns:fx="http://javafx.com/fxml">
               <Label fx:id="label1" text="Hi!"/>
            </Pane>
            """,
            List.<Statement>of(
                // Pane pane = new Pane()
                VariableDeclaration.instantiation("javafx.scene.layout.Pane", "pane"),
                    // Label label = new Label()
                    VariableDeclaration.instantiation("javafx.scene.control.Label", "label"),
                        // label.setId("label1")
                        new MethodCall("label", "setId", Literal.string("label1")),
                        // label.setText("Hi!")
                        new MethodCall("label", "setText", Literal.string("Hi!")),
                // pane.getChildren().add(label)
                new MethodCall(
                    new MethodCall("pane", "getChildren"),
                    "add",
                    new VariableExpression("label")
                )
            )
        );
    }
}

/*
*/