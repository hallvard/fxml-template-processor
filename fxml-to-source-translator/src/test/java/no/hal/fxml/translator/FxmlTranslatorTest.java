package no.hal.fxml.translator;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import no.hal.fxml.model.FxmlCode.Document;
import no.hal.fxml.model.JavaCode.ClassDeclaration;
import no.hal.fxml.model.JavaCode.ClassTarget;
import no.hal.fxml.model.JavaCode.ConstructorCall;
import no.hal.fxml.model.JavaCode.ConstructorDeclaration;
import no.hal.fxml.model.JavaCode.ExpressionTarget;
import no.hal.fxml.model.JavaCode.FieldAssignment;
import no.hal.fxml.model.JavaCode.GetFxmlObjectCall;
import no.hal.fxml.model.JavaCode.LambdaExpression;
import no.hal.fxml.model.JavaCode.Literal;
import no.hal.fxml.model.JavaCode.MethodCall;
import no.hal.fxml.model.JavaCode.MethodDeclaration;
import no.hal.fxml.model.JavaCode.Return;
import no.hal.fxml.model.JavaCode.SetFxmlObjectCall;
import no.hal.fxml.model.JavaCode.Statement;
import no.hal.fxml.model.JavaCode.VariableDeclaration;
import no.hal.fxml.model.JavaCode.VariableExpression;
import no.hal.fxml.model.QName;
import no.hal.fxml.model.TypeRef;
import no.hal.fxml.parser.FxmlParser;

public class FxmlTranslatorTest {

    private void testFxmlTranslator(ClassDeclaration actual, ClassDeclaration expected) throws Exception {
        Assertions.assertEquals(expected, actual,
            actual + "\nvs.\n" + expected
        );
    }

    private void testFxmlTranslator(Document fxmlDoc, ClassDeclaration expectedBuilder) throws Exception {
        FxmlTranslator.FxmlTranslation actual = null;
        try {
            actual = FxmlTranslator.translateFxml(fxmlDoc, getClass().getClassLoader());
        } catch(Exception ex) {
            ex.printStackTrace();
        } finally {
            System.out.println(
                FxmlTranslator.toJavaSource(actual.builderClass())
            );
        }
        testFxmlTranslator(actual.builderClass(), expectedBuilder);
    }

    public static class Controller {

        @FXML
        javafx.scene.paint.Color red;
    
        javafx.scene.control.Label label1;

        @FXML
        void setLabel1(javafx.scene.control.Label label1) {
            this.label1 = label1;
        }

        @FXML
        void onAnswerInput(ActionEvent event) {
            System.out.println(event);
        }
    }

    private static String FXML_SAMPLE = """
        <?import javafx.scene.control.*?>
        <?import javafx.scene.layout.*?>
        <?import javafx.scene.paint.*?>
        <?import javafx.scene.shape.*?>
        <Pane xmlns:fx="http://javafx.com/fxml" fx:controller="no.hal.fxml.translator.FxmlTranslatorTest$Controller">
            <fx:define>
                <String fx:id="prompt" fx:value="Enter answer"/>
                <TextField fx:id="answerInput" promptText="$prompt" onAction="#onAnswerInput"/>
                <Color fx:id="red" red="1.0" green="0.0" blue="0.0"/>
            </fx:define>
            <Label fx:id="label1" text="Hi!"/>
            <fx:reference source="answerInput"/>
            <Rectangle fill="$red"/>
        </Pane>
        """;

    @Test
    public void testFxmlTranslator() throws Exception {
        testFxmlTranslator(FxmlParser.parseFxml(FXML_SAMPLE),
            new ClassDeclaration(
                QName.valueOf("no.hal.fxml.translator.TestOutput"),
                TypeRef.valueOf("no.hal.fxml.builder.AbstractFxLoader<javafx.scene.layout.Pane, no.hal.fxml.translator.FxmlTranslatorTest$Controller>"),
                null,
                List.of(
                    new ConstructorDeclaration("public", "TestOutput", List.of()),
                    new ConstructorDeclaration("public", "TestOutput", List.of(
                        new VariableDeclaration(TypeRef.valueOf("java.util.Map<String, Object>"), "namespace", null)
                    )),
                    new MethodDeclaration("protected", "build", TypeRef.valueOf("javafx.scene.layout.Pane"), List.of(),
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
                                    // textField.setOnAction((event) -> hash_onAnswerInput(event))
                                    new MethodCall("textField", "setOnAction",
                                        new LambdaExpression("event", new MethodCall(new ExpressionTarget("this.controller"), "onAnswerInput", new VariableExpression("event")))
                                    ),
                                    // textField.setText(getFxmlObject("prompt"))
                                    new MethodCall("textField", "setPromptText", new GetFxmlObjectCall("prompt")),
                                // Color color = new Color(1.0, 0.0, 0.0, 1.0)
                                new VariableDeclaration("javafx.scene.paint.Color", "color",
                                    new ConstructorCall(QName.valueOf("javafx.scene.paint.Color"), List.of(
                                        new Literal("1.0", Double.TYPE),
                                        new Literal("0.0", Double.TYPE),
                                        new Literal("0.0", Double.TYPE),
                                        new Literal("1", Double.TYPE)
                                    ))
                                ),
                                new SetFxmlObjectCall("red", "color"),
                                // Label label = new Label()
                                VariableDeclaration.instantiation("javafx.scene.control.Label", "label"),
                                    new SetFxmlObjectCall("label1", "label"),
                                    // label.setId("label1")
                                    new MethodCall("label", "setId", Literal.string("label1")),
                                    // label.setText("Hi!")
                                    new MethodCall("label", "setText", Literal.string("Hi!")),
                                    // pane.getChildren().add(label)
                                    new MethodCall(new MethodCall("pane", "getChildren"), "add", new VariableExpression("label")),
                                    // pane.getChildren().add(getFxmlObject("answerInput"))
                                    new MethodCall(new MethodCall("pane", "getChildren"), "add", new GetFxmlObjectCall("answerInput")),
                                VariableDeclaration.instantiation("javafx.scene.shape.Rectangle", "rectangle"),
                                    // rectangle.setFill(red)
                                    new MethodCall("rectangle", "setFill", new GetFxmlObjectCall("red")),
                                    // pane.getChildren().add(rectangle)
                                    new MethodCall(new MethodCall("pane", "getChildren"), "add", new VariableExpression("rectangle")),
                                new Return(new VariableExpression("pane"))
                        )
                    ),
                    new MethodDeclaration("protected", "createController", new TypeRef(new QName("no.hal.fxml.translator", "FxmlTranslatorTest.Controller")), null, List.of(
                        new Return(new ConstructorCall(new QName("no.hal.fxml.translator", "FxmlTranslatorTest.Controller")))
                    )),
                    new MethodDeclaration("protected", "initializeController", null, null, List.of(
                        new MethodCall(new ConstructorCall(new QName("no.hal.fxml.translator", "FxmlTranslatorTest.ControllerHelper"), List.of(new MethodCall("this", "getNamespace"), new VariableExpression("this.controller"))),
                            "initializeController")
                    ))
                )
            )
       );
    }
}

/*

*/