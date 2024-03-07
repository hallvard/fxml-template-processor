package no.hal.fxml.translator;

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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;

import no.hal.fxml.model.JavaCode.ClassDeclaration;
import no.hal.fxml.model.JavaCode.ConstructorDeclaration;
import no.hal.fxml.model.JavaCode.ExecutableCall;
import no.hal.fxml.model.JavaCode.Expression;
import no.hal.fxml.model.JavaCode.ExpressionTarget;
import no.hal.fxml.model.JavaCode.FieldAssignment;
import no.hal.fxml.model.JavaCode.Member;
import no.hal.fxml.model.JavaCode.MethodCall;
import no.hal.fxml.model.JavaCode.MethodDeclaration;
import no.hal.fxml.model.JavaCode.ObjectTarget;
import no.hal.fxml.model.JavaCode.Statement;
import no.hal.fxml.model.JavaCode.VariableDeclaration;
import no.hal.fxml.model.JavaCode.VariableExpression;
import no.hal.fxml.model.JavaCode;
import no.hal.fxml.model.QName;
import no.hal.fxml.model.TypeRef;

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
                    out.write(JavaCode.toJavaSource(classDeclaration));
                } catch (IOException ioex) {
                    System.err.println("Exception writing " + classDeclaration.className() + ": " + ioex);
                }
            });
        return true;
    }

    private ClassDeclaration generateControllerHelper(Map.Entry<Element, Collection<Element>> typeElementAnnotations) {
        QName controllerClassName = QName.valueOf(typeElementAnnotations.getKey().toString());
        String helperClassName = controllerClassName.className() + "Helper";
        List<Member> members = new ArrayList<>();
        members.add(new ConstructorDeclaration("public", helperClassName, List.of(
            VariableDeclaration.parameter(TypeRef.valueOf("java.util.Map<String, Object>"), "namespace"),
            VariableDeclaration.parameter(new TypeRef(controllerClassName), "controller")
        ), null));
        members.add(generateInitializeMethod(typeElementAnnotations));
        members.addAll(generateEventHandlers(typeElementAnnotations));
        return new ClassDeclaration(QName.valueOf(typeElementAnnotations.getKey() + "Helper"), new TypeRef(QName.valueOf("no.hal.fxml.runtime.AbstractFxControllerHelper"), new TypeRef(controllerClassName)), null, members);
    }

    private MethodDeclaration generateInitializeMethod(Map.Entry<Element, Collection<Element>> typeElementAnnotations) {
        List<Statement> statements = new ArrayList<>();
        for (var member : typeElementAnnotations.getValue()) {
            var name = member.getSimpleName().toString();
            switch (member.getKind()) {
                case ElementKind.FIELD -> {
                    // initialize with assignment
                    var fieldAssignment = new FieldAssignment("this.controller", name, FxmlTranslator.getFxmlObjectCall(name));
                    statements.add(fieldAssignment);
                }
                case ElementKind.METHOD -> {
                    String propertyName = propertyName("set", name);
                    if (propertyName != null) {
                        // initialize with method call
                        var methodCall = new MethodCall("this.controller", name, FxmlTranslator.getFxmlObjectCall(propertyName));
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
        return new MethodDeclaration("public", "initializeController", null, null, statements);
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

    private List<MethodDeclaration> generateEventHandlers(Map.Entry<Element, Collection<Element>> typeElementAnnotations) {
        List<MethodDeclaration> eventHandlers = new ArrayList<>();
        for (var member : typeElementAnnotations.getValue()) {
            var name = member.getSimpleName().toString();
            if (member.getKind() == ElementKind.METHOD && (! "initialize".equals(name)) && propertyName("set", name) == null) {
                if (member.asType() instanceof ExecutableType execType) {
                    var params = execType.getParameterTypes();
                    if (params.size() > 1) {
                        throw new IllegalArgumentException("Event handlers must have 0 or 1 argument");
                    }
                    // method declaration with event argument of appropriate type, that calls controller's method
                    TypeRef paramType = (params.size() > 0
                        ? new TypeRef(params.get(0).toString())
                        : TypeRef.valueOf("javafx.event.Event")
                    );
                    var param = new VariableDeclaration(paramType, "event", null);
                    var method = new MethodDeclaration("public", name, null, List.of(param), List.of(
                        new MethodCall(new ExpressionTarget("this.controller"), name, (params.size() > 0 ? new VariableExpression("event") : null)))
                    );
                    eventHandlers.add(method);
                }
            }
        }
        return eventHandlers;
    }
}
