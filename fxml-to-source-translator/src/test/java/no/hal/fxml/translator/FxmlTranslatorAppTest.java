package no.hal.fxml.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.ICompiler;
import org.codehaus.commons.compiler.util.ResourceFinderClassLoader;
import org.codehaus.commons.compiler.util.resource.MapResourceCreator;
import org.codehaus.commons.compiler.util.resource.MapResourceFinder;
import org.codehaus.commons.compiler.util.resource.Resource;
import org.codehaus.commons.compiler.util.resource.StringResource;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.matcher.control.LabeledMatchers;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import no.hal.fxml.model.FxmlCode.Document;
import no.hal.fxml.model.JavaCode;
import no.hal.fxml.model.JavaCode.ClassDeclaration;
import no.hal.fxml.model.QName;
import no.hal.fxml.parser.FxmlParser;
import no.hal.fxml.runtime.AbstractFxControllerHelper;
import no.hal.fxml.runtime.FxLoader;

public class FxmlTranslatorAppTest extends ApplicationTest {
    
    private QName className = QName.valueOf("no.hal.fxml.translator.TestOutput");

    private FxLoader<?, ?> fxLoader;

    @Override
    public void start(Stage stage) throws Exception {
        Document fxmlDoc = FxmlParser.parseFxml(FXML_SAMPLE);
        FxmlTranslator.Config config = new FxmlTranslator.Config(true, false, true);
        var cl = this.getClass().getClassLoader();
        ClassDeclaration fxLoaderClass = FxmlTranslator.translateFxml(fxmlDoc, className, cl, config);
        String javaSource = JavaCode.toJavaSource(fxLoaderClass);
        System.out.println(javaSource);

        ICompiler compiler = CompilerFactoryFactory.getDefaultCompilerFactory(cl).newCompiler();
        // Store generated .class files in a Map:
        Map<String, byte[]> classes = new HashMap<String, byte[]>();
        compiler.setClassFileCreator(new MapResourceCreator(classes));
 
        // Now compile two units from strings:
        compiler.compile(new Resource[] {
            new StringResource("no/hal/fxml/translator/TestOutput.java", javaSource)
        });
        ClassLoader cl2 = new ResourceFinderClassLoader(new MapResourceFinder(classes), cl);
        fxLoader = (FxLoader<?, ?>) cl2.loadClass(className.toString()).getConstructor().newInstance();
        Pane pane = (Pane) fxLoader.load(null);
        stage.setScene(new Scene(pane));
        stage.show();
    }

    @Test
    public void checkController() {
        if (fxLoader.getController() instanceof Controller ctrler) {
            assertEquals(fxLoader.getNamespace().get("red"), ctrler.red);
            assertEquals(fxLoader.getNamespace().get("label1"), ctrler.label1);
            assertEquals(lookup("#label1").query(), ctrler.label1);
        } else {
            fail("controller isn't Controller");
        }
    }

    @Test
    public void checkControls() {
        FxAssert.verifyThat(".label", LabeledMatchers.hasText("Hi!"));
        FxAssert.verifyThat("#label1", LabeledMatchers.hasText("Hi!"));
        if (lookup("#answerInput").query() instanceof TextField textField) {
            assertEquals("Enter answer", textField.getPromptText());
        } else {
            fail("#answerInput isn't TextField");
        }
    }
    
    private static String FXML_SAMPLE = """
        <?import javafx.scene.control.*?>
        <?import javafx.scene.layout.*?>
        <?import javafx.scene.paint.*?>
        <?import javafx.scene.shape.*?>
        <Pane xmlns:fx="http://javafx.com/fxml" fx:controller="no.hal.fxml.translator.FxmlTranslatorAppTest$Controller">
            <fx:define>
                <String fx:id="prompt" fx:value="Enter answer"/>
                <TextField fx:id="answerInput" promptText="$prompt"/>
                <Color fx:id="red" red="1.0" green="0.0" blue="0.0"/>
            </fx:define>
            <Label fx:id="label1" text="Hi!"/>
            <fx:reference source="answerInput"/>
            <Rectangle fill="$red"/>
        </Pane>
        """;

    public static class Controller {
        @FXML Color red;

        Label label1;
        @FXML
        void setLabel1(Label label1) {
            this.label1 = label1;
        }

        @FXML
        void initialize() {
        }
    }    

    // normally generated by annotation processor
    public static class ControllerHelper extends AbstractFxControllerHelper<FxmlTranslatorAppTest.Controller> {
        
        public ControllerHelper(Map<String, Object> namespace, FxmlTranslatorAppTest.Controller controller) {
           super(namespace, controller);
        }
     
        public void initializeController() {
           this.controller.red = this.getFxmlObject("red");
           this.controller.setLabel1(this.getFxmlObject("label1"));
           this.controller.initialize();
        }
    }
}
