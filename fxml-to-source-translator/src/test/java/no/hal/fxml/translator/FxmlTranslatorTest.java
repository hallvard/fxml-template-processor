package no.hal.fxml.translator;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import no.hal.fxml.model.FxmlCode.Document;
import no.hal.fxml.model.JavaCode;
import no.hal.fxml.model.JavaCode.ClassTarget;
import no.hal.fxml.model.JavaCode.ExpressionTarget;
import no.hal.fxml.model.JavaCode.GetFxmlObjectCall;
import no.hal.fxml.model.JavaCode.Literal;
import no.hal.fxml.model.JavaCode.MethodCall;
import no.hal.fxml.model.JavaCode.SetFxmlObjectCall;
import no.hal.fxml.model.JavaCode.Statement;
import no.hal.fxml.model.JavaCode.VariableDeclaration;
import no.hal.fxml.model.JavaCode.VariableExpression;
import no.hal.fxml.parser.FxmlParser;

public class FxmlTranslatorTest {

    private void testFxmlTranslator(Document fxmlDoc, List<Statement> expected) throws Exception {
        List<Statement> actual = null;
        try {
            actual = FxmlTranslator.translateFxml(fxmlDoc, getClass().getClassLoader()).statements();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        Assertions.assertEquals(expected, actual,
            JavaCode.statements2String(expected) + "\nvs.\n" + JavaCode.statements2String(actual)
        );
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
                <fx:define>
                    <String fx:id="prompt" fx:value="Enter answer"/>
                    <TextField fx:id="answerInput" promptText="$prompt"/>
                </fx:define>
               <Label fx:id="label1" text="Hi!"/>
               <fx:reference source="answerInput"/>
            </Pane>
            """,
            List.<Statement>of(
                // Pane pane = new Pane()
                VariableDeclaration.instantiation("javafx.scene.layout.Pane", "pane"),
                    // String string = String.valueOf("Enter answer")
                    new VariableDeclaration("java.lang.String", "string",
                        new MethodCall(new ClassTarget("java.lang.String"), "valueOf", List.of(Literal.string("Enter answer")))
                    ),
                        new SetFxmlObjectCall("prompt", "string"),
                    // TextField textField = new TextField()
                    VariableDeclaration.instantiation("javafx.scene.control.TextField", "textField"),
                        new SetFxmlObjectCall("answerInput", "textField"),
                        // textField.setId("answerInput")
                        new MethodCall("textField", "setId", Literal.string("answerInput")),
                        // textField.setText(getFxmlObject("prompt"))
                        new MethodCall("textField", "setPromptText", new GetFxmlObjectCall("prompt")),
                    // Label label = new Label()
                    VariableDeclaration.instantiation("javafx.scene.control.Label", "label"),
                        new SetFxmlObjectCall("label1", "label"),
                        // label.setId("label1")
                        new MethodCall("label", "setId", Literal.string("label1")),
                        // label.setText("Hi!")
                        new MethodCall("label", "setText", Literal.string("Hi!")),
                    new MethodCall(
                        // pane.getChildren().add(label)
                        new MethodCall("pane", "getChildren"),
                        "add",
                        new VariableExpression("label")
                    ),
                    // pane.getChildren().add(getFxmlObject("answerInput"))
                    new MethodCall(new MethodCall("pane", "getChildren"), "add", new GetFxmlObjectCall("answerInput"))
            )
        );
    }
}

/*
*/