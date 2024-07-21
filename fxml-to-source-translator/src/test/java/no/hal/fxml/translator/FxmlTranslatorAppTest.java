package no.hal.fxml.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
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
import no.hal.fxml.model.JavaCode;
import no.hal.fxml.model.JavaCode.ClassDeclaration;
import no.hal.fxml.model.QName;
import no.hal.fxml.parser.FxmlParser;
import no.hal.fxml.runtime.AbstractFxControllerHelper;
import no.hal.fxml.runtime.DefaultFxLoaderContext;
import no.hal.fxml.runtime.DefaultFxLoaderProvider;
import no.hal.fxml.runtime.FxLoader;
import no.hal.fxml.runtime.FxLoaderContext;
import no.hal.fxml.runtime.FxLoaderProvider;

public class FxmlTranslatorAppTest extends ApplicationTest {
    
    private String packageName = "no.hal.fxml.translator";
    private QName className = new QName(packageName, "FxLoader1");
    private QName classNameIncluded = new QName(packageName, "FxLoader1Included");
    private QName fxLoaderProviderClassName = new QName(packageName, "FxLoader1Provider");

    private FxLoader<?, ?> fxLoader;

    @Override
    public void start(Stage stage) throws Exception {
        FxmlTranslator.Config config = new FxmlTranslator.Config(true, false, true);
        var cl = this.getClass().getClassLoader();
        ClassDeclaration fxLoaderClass = FxmlTranslator.translateFxml(FxmlParser.parseFxml(FXML_SAMPLE), className, cl, config);
        String javaSource = JavaCode.toJavaSource(fxLoaderClass);
        ClassDeclaration fxLoaderClassIncluded = FxmlTranslator.translateFxml(FxmlParser.parseFxml(FXML_SAMPLE_INCLUDED), classNameIncluded, cl, config);
        String javaSourceIncluded = JavaCode.toJavaSource(fxLoaderClassIncluded);

        ClassDeclaration fxLoaderProvider = new FxLoaderProviderGenerator().generateFxLoaderProvider(fxLoaderProviderClassName, List.of(
            new FxmlTranslator.FxmlTranslation(Path.of(name2Resource(packageName) + "/included.fxml"), fxLoaderClassIncluded)
        ));
        String fxLoaderProviderSource = JavaCode.toJavaSource(fxLoaderProvider);

        ICompiler compiler = CompilerFactoryFactory.getDefaultCompilerFactory(cl).newCompiler();
        // Store generated .class files in a Map:
        Map<String, byte[]> classes = new HashMap<String, byte[]>();
        compiler.setClassFileCreator(new MapResourceCreator(classes));
 
        // Now compile two units from strings:
        compiler.compile(new Resource[] {
            new StringResource(className2Resource(className), javaSource),
            new StringResource(className2Resource(classNameIncluded), javaSourceIncluded)
            // new StringResource(className2Resource(fxLoaderProviderClassName), fxLoaderProviderSource)
        });
        ClassLoader cl2 = new ResourceFinderClassLoader(new MapResourceFinder(classes), cl);
        fxLoader = (FxLoader<?, ?>) cl2.loadClass(className.toString()).getConstructor().newInstance();
        FxLoader<?, ?> fxLoaderIncluded = (FxLoader<?, ?>) cl2.loadClass(classNameIncluded.toString()).getConstructor().newInstance();

        FxLoaderContext fxLoaderContext = new DefaultFxLoaderContext(Path.of(name2Resource(packageName)),
            // (FxLoaderProvider) cl2.loadClass(fxLoaderProviderClassName.toString()).getConstructor().newInstance()
            new DefaultFxLoaderProvider(Map.of(Path.of(name2Resource(packageName) + "/included.fxml"), () -> fxLoaderIncluded))
        );
        Pane pane = (Pane) fxLoader.load(fxLoaderContext);
        stage.setScene(new Scene(pane));
        stage.show();
    }

    private String name2Resource(String name) {
        return name.replace('.', '/');
    }
    private String className2Resource(QName className) {
        return name2Resource(className.toString()) + ".java";
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

    @Test
    public void checkControlsIncluded() {
        assertTrue(fxLoader.getNamespace().get("included") instanceof Label);
        FxAssert.verifyThat("#includedLabel", LabeledMatchers.hasText("included"));
        if (lookup("#includedLabel").query() instanceof Label label) {
            assertEquals("included", label.getText());
        } else {
            fail("#included isn't Label");
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
            <fx:include fx:id="included" source="included.fxml"/>
        </Pane>
        """;

        private static String FXML_SAMPLE_INCLUDED = """
        <?import javafx.scene.control.Label?>
        <Label fx:id="includedLabel" xmlns:fx="http://javafx.com/fxml" text="included"/>
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
