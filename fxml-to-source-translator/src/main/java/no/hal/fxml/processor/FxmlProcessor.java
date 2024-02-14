package no.hal.fxml.processor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import no.hal.fxml.model.JavaCode.ClassDeclaration;
import no.hal.fxml.model.JavaCode.ConstructorDeclaration;
import no.hal.fxml.model.JavaCode.FieldAssignment;
import no.hal.fxml.model.JavaCode.GetFxmlObjectCall;
import no.hal.fxml.model.JavaCode.MethodCall;
import no.hal.fxml.model.JavaCode.MethodDeclaration;
import no.hal.fxml.model.JavaCode.Statement;
import no.hal.fxml.model.JavaCode.VariableDeclaration;
import no.hal.fxml.model.QName;
import no.hal.fxml.model.TypeRef;
import no.hal.fxml.translator.FxmlTranslator;

@SupportedAnnotationTypes({
    "javafx.fxml.FXML"
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class FxmlProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<Element, Collection<Element>> fxmlAnnotations = new HashMap<>();
        for (var annotation : annotations) {
            for (var elt : roundEnv.getElementsAnnotatedWith(annotation)) {
                Element typeElement = switch(elt.getKind()) {
                    case FIELD -> elt.getEnclosingElement();
                    case METHOD ->  elt.getEnclosingElement();
                    default -> null;
                };
                if (typeElement != null && typeElement.getSimpleName().toString().endsWith("Controller")) {
                    var elements = fxmlAnnotations.get(typeElement);
                    if (elements == null) {
                        elements = new ArrayList<>();
                        fxmlAnnotations.put(typeElement, elements);
                    }
                    elements.add(elt);
                }
            }
        }
        fxmlAnnotations.entrySet().stream()
            .map(this::generateControllerHelper)
            .forEach(classDeclaration -> {
                try (PrintWriter out = new PrintWriter(processingEnv.getFiler().createSourceFile(classDeclaration.className().toString()).openWriter())) {
                    out.write(FxmlTranslator.toJavaSource(classDeclaration));
                } catch (IOException ioex) {
                    System.err.println("Exception writing " + classDeclaration.className() + ": " + ioex);
                }
            });
        return true;
    }

    private ClassDeclaration generateControllerHelper(Map.Entry<Element, Collection<Element>> typeElementAnnotations) {
        List<Statement> statements = new ArrayList<>();
        for (var member : typeElementAnnotations.getValue()) {
            var name = member.getSimpleName().toString();
            switch (member.getKind()) {
                case ElementKind.FIELD -> {
                    var fieldAssignment = new FieldAssignment("this.controller", name, new GetFxmlObjectCall(name));
                    statements.add(fieldAssignment);
                }
                case ElementKind.METHOD -> {
                    String propertyName = propertyName("set", name);
                    if (propertyName != null) {
                        var methodCall = new MethodCall("this.controller", name, new GetFxmlObjectCall(propertyName));
                        statements.add(methodCall);
                    }
                }
                default -> {}
            }
        }
        typeElementAnnotations.getValue().stream()
            .filter(m -> m.getKind() == ElementKind.METHOD && "initialize".equals(m.getSimpleName().toString()))
            .findAny()
            .ifPresent(m -> statements.add(new MethodCall("this.controller", "initialize")));

        QName controllerClassName = QName.valueOf(typeElementAnnotations.getKey().toString());
        QName helperClassName = QName.valueOf(controllerClassName + "Helper");
        ConstructorDeclaration constructor = new ConstructorDeclaration("public", helperClassName.className(), List.of(
            new VariableDeclaration(TypeRef.valueOf("java.util.Map<String, Object>"), "namespace", null),
            new VariableDeclaration(controllerClassName, "controller", null)
        ), null);
        MethodDeclaration initializeMethod = new MethodDeclaration("public", "initializeController", null, null, statements);
        return new ClassDeclaration(QName.valueOf(typeElementAnnotations.getKey() + "Helper"), new TypeRef(QName.valueOf("no.hal.fxml.builder.AbstractFxControllerHelper"), new TypeRef(controllerClassName)), null, List.of(
            constructor,
            initializeMethod
        ));
    }

    private static String propertyName(String prefix, String methodName) {
        if (methodName.startsWith(prefix)) {
            return (Character.isUpperCase(methodName.charAt(prefix.length()))
                ? Character.toLowerCase(methodName.charAt(prefix.length())) + methodName.substring(prefix.length() + 1)
                : methodName.substring(prefix.length())
            );
        }
        return null;
    }
}
