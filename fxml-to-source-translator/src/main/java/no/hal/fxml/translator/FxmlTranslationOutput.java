package no.hal.fxml.translator;

import java.util.Map;

import no.hal.fxml.model.JavaCode;
import no.hal.fxml.model.JavaCode.Formatter;
import no.hal.fxml.model.JavaCode.Imports;
import no.hal.fxml.model.QName;
import no.hal.fxml.translator.FxmlTranslator.FxmlTranslation;

public class FxmlTranslationOutput {

    public static String toFxmlBuilderSource(FxmlTranslation translation, QName className) {
        Imports imports = new Imports(Map.<String, QName>of());
        JavaCode.Formatter formatter = new Formatter(imports);
        formatter.append("""
            package %s;

            import java.util.Map;
            import no.hal.fxml.builder.AbstractFxBuilder;

            """.formatted(className.packageName())
        );

        String classDecl = formatter.format(translation, (f, t) -> {
            QName nodeType = t.builder().returnType();
            String controllerTypeString = f.toString(QName.valueOf(translation.controllerClass().getName()));
            String nodeTypeString = f.toString(nodeType != null ? nodeType : QName.valueOf("javax.scene.Node"));
        
            f.append("""

                public class %s extends AbstractFxBuilder<%s, %s> {
    
                   public %s() {
                      super();
                   }

                   public %s(Map<String, Object> namespace) {
                      super(namespace);
                   }

                """.formatted(className.className(), nodeTypeString, controllerTypeString, className.className(), className.className())
            );
            f.withIndentation(t.builder(), Formatter::format);
            if (t.initializer() != null) {
                f.newline();
                f.withIndentation(t.initializer(), Formatter::format);
            }
            f.append("}");
        });
        
        formatter.format(imports);
        formatter.append(classDecl);

        return formatter.toString();
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
