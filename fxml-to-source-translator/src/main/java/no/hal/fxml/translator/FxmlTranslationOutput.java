package no.hal.fxml.translator;

import java.util.Map;

import javafx.fxml.FXML;
import no.hal.fxml.model.JavaCode;
import no.hal.fxml.model.JavaCode.Imports;
import no.hal.fxml.model.QName;
import no.hal.fxml.translator.FxmlTranslator.FxmlTranslation;

public class FxmlTranslationOutput {

    public static String toFxmlBuilderSource(FxmlTranslation translation, QName className) {
        Imports imports = new Imports(Map.<String, QName>of());
        JavaCode.Formatter formatter = new JavaCode.Formatter(imports);
        QName nodeType = translation.builder().returnType();
        String nodeTypeString = formatter.toString(nodeType != null ? nodeType : QName.valueOf("javax.scene.Node"));
        String extraImports = formatter.format(imports);
    
        return """
            package %s;

            import java.util.Map;
            import no.hal.fxml.builder.AbstractFxBuilder;

            %s

            public class %s extends AbstractFxBuilder<%s> {

                public %s() {
                    super();
                }
                public %s(Map<String, Object> namespace) {
                    super(namespace);
                }
                
                %s

                %s
            }
            """
            .formatted(
                className.packageName(), // package %s
                extraImports,
                className.className(), // public class %s
                nodeTypeString, // extends AbstractFxBuilder<%s>
                className.className(), // public %s()
                className.className(), // public %s(...)
                translation.builder(),
                translation.initializer()
            );
    }

    public static void main(String[] args) throws Exception {

        var translation = FxmlTranslator.translateFxml("""
            <?import javafx.scene.control.*?>
            <?import javafx.scene.layout.*?>
            <?import javafx.scene.paint.*?>
            <?import javafx.scene.shape.*?>
            <Pane xmlns:fx="http://javafx.com/fxml">
                <fx:define>
                    <String fx:id="prompt" fx:value="Enter answer"/>
                    <TextField fx:id="answerInput" promptText="$prompt"/>
                    <Color fx:id="red" red="1.0" green="0.0" blue="0.0" opacity="1.0"/>
                </fx:define>
               <Label fx:id="label1" text="Hi!"/>
               <fx:reference source="answerInput"/>
               <Rectangle x="0.0" y="0.0" width="100.0" height="100.0" fill="$red"/>
            </Pane>
            """, FxmlTranslator.class.getClassLoader());
        System.out.println(toFxmlBuilderSource(translation, QName.valueOf("no.hal.fxml.translator.TestOutput")));
    }
}
