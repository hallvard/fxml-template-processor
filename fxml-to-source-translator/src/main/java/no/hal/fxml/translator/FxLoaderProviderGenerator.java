package no.hal.fxml.translator;

import java.util.List;

import no.hal.fxml.model.JavaCode.ClassDeclaration;
import no.hal.fxml.model.JavaCode.ClassTarget;
import no.hal.fxml.model.JavaCode.ConstructorCall;
import no.hal.fxml.model.JavaCode.ConstructorDeclaration;
import no.hal.fxml.model.JavaCode.Expression;
import no.hal.fxml.model.JavaCode.LambdaExpression;
import no.hal.fxml.model.JavaCode.Literal;
import no.hal.fxml.model.JavaCode.MethodCall;
import no.hal.fxml.model.JavaCode.ObjectTarget;
import no.hal.fxml.model.QName;
import no.hal.fxml.model.TypeRef;
import no.hal.fxml.translator.FxmlTranslator.FxmlTranslation;

public class FxLoaderProviderGenerator {

    public ClassDeclaration generateFxLoaderProvider(QName providerClassName, List<FxmlTranslation> translations) {
        ClassDeclaration providerClass = new ClassDeclaration(providerClassName, TypeRef.valueOf("no.hal.fxml.runtime.DefaultFxLoaderProvider"), null, List.of(
            new ConstructorDeclaration("public", providerClassName.className(), List.of(), List.of(
                new MethodCall((ObjectTarget) null, "super",
                    new MethodCall(new ClassTarget("java.util.Map"), "of",
                        translations.stream().flatMap(translation -> List.<Expression>of(
                            new MethodCall(new ClassTarget("java.nio.file.Path"), "of", Literal.string(translation.path().toString())),
                            new LambdaExpression(List.of(), new ConstructorCall(translation.builderClass().className()))
                        ).stream()).toList()
                    )
                )
            )
        )));
        return providerClass;
    }
}
