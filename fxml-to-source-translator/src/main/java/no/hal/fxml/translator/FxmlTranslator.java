package no.hal.fxml.translator;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.hal.fxml.model.FxmlCode.BeanElement;
import no.hal.fxml.model.FxmlCode.BeanProperty;
import no.hal.fxml.model.FxmlCode.Define;
import no.hal.fxml.model.FxmlCode.Document;
import no.hal.fxml.model.FxmlCode.FxmlElement;
import no.hal.fxml.model.FxmlCode.Include;
import no.hal.fxml.model.FxmlCode.InstanceElement;
import no.hal.fxml.model.FxmlCode.InstantiationElement;
import no.hal.fxml.model.FxmlCode.PropertyElement;
import no.hal.fxml.model.FxmlCode.PropertyValue;
import no.hal.fxml.model.FxmlCode.Reference;
import no.hal.fxml.model.FxmlCode.Root;
import no.hal.fxml.model.FxmlCode.StaticProperty;
import no.hal.fxml.model.Instantiation;
import no.hal.fxml.model.Instantiation.Constant;
import no.hal.fxml.model.Instantiation.Constructor;
import no.hal.fxml.model.Instantiation.Factory;
import no.hal.fxml.model.Instantiation.Value;
import no.hal.fxml.model.JavaCode;
import no.hal.fxml.model.JavaCode.ClassTarget;
import no.hal.fxml.model.JavaCode.ConstructorCall;
import no.hal.fxml.model.JavaCode.Expression;
import no.hal.fxml.model.JavaCode.ExpressionTarget;
import no.hal.fxml.model.JavaCode.GetFxmlObjectCall;
import no.hal.fxml.model.JavaCode.Literal;
import no.hal.fxml.model.JavaCode.MethodCall;
import no.hal.fxml.model.JavaCode.MethodTarget;
import no.hal.fxml.model.JavaCode.Return;
import no.hal.fxml.model.JavaCode.SetFxmlObjectCall;
import no.hal.fxml.model.JavaCode.Statement;
import no.hal.fxml.model.JavaCode.VariableDeclaration;
import no.hal.fxml.model.JavaCode.VariableExpression;
import no.hal.fxml.model.QName;
import no.hal.fxml.model.ValueExpression;
import no.hal.fxml.parser.FxmlParser;

public class FxmlTranslator {
    
    private final ClassResolver classResolver;
    private final ReflectionHelper reflectionHelper;

    public FxmlTranslator(Document fxmlDocument, ClassLoader classLoader) {
        this.classResolver = new ClassResolver(classLoader, fxmlDocument.imports());
        this.reflectionHelper = new ReflectionHelper();
    }

    private List<Statement> statements = new ArrayList<>();
    private Map<FxmlElement, Expression> expressions = new HashMap<>();

    private void emit(Statement statement) {
        statements.add(statement);
    }

    private Expression expressionFor(FxmlElement fxmlElement, Expression expression) {
        expressions.put(fxmlElement, expression);
        return expression;
    }
    private Expression expressionFor(FxmlElement fxmlElement) {
        return expressions.get(fxmlElement);
    }

    private Map<String, Integer> variableMap = new HashMap<>();

    private String gensym(String baseName) {
        if (Character.isUpperCase(baseName.charAt(0))) {
            baseName = Character.toLowerCase(baseName.charAt(0)) + baseName.substring(1);
        }
        int varNum = variableMap.computeIfAbsent(baseName, varName -> 0);
        variableMap.put(baseName, varNum + 1);
        return varNum == 0 ? baseName : baseName + varNum;
    }

    public record FxmlTranslation(List<Statement> statements, Expression rootExpression) {
    }

    public static FxmlTranslation translateFxml(Document fxmlDocument, ClassLoader classLoader) {
        FxmlTranslator translator = new FxmlTranslator(fxmlDocument, classLoader);
        Expression rootExpression = translator.translateFxml(fxmlDocument.instanceElement());
        translator.emit(new Return(rootExpression));
        return new FxmlTranslation(translator.statements, rootExpression);
    }
    public static FxmlTranslation translateFxml(String fxml, ClassLoader classLoader) throws Exception {
        return translateFxml(FxmlParser.parseFxml(fxml), classLoader);
    }
    public static FxmlTranslation translateFxml(InputStream input, ClassLoader classLoader) throws Exception {
        return translateFxml(FxmlParser.parseFxml(input), classLoader);
    }

    private Expression translateFxml(FxmlElement fxmlElement) {
        return switch (fxmlElement) {
            case Define define -> {
                define.children().forEach(this::translateFxml);
                yield null;
            }
            case Root root -> {
                var rootVariable = gensym("root");
                emit(new VariableDeclaration(root.typeName(), rootVariable, new VariableExpression("_fxmlLoader.getRoot()")));
                var rootExpression = expressionFor(root, new VariableExpression(rootVariable));
                translateBeanChildren(root, root.children());
                yield rootExpression;
            }
            case InstantiationElement instantiationElement -> {
                var instanceExpression = translateInstantiation(instantiationElement.className(), instantiationElement.instantiation());
                if (instanceExpression != null) {
                    expressionFor(instantiationElement, instanceExpression);
                    translateId(instantiationElement);
                    translateBeanChildren(instantiationElement, instantiationElement.children());
                } else {
                    // try constructor with @NamedArg, e.g. Color(red, green, blue))
                    // how to support other JavaFXBuilderFactory logic???
                }
                yield instanceExpression;
            }
            case Reference reference -> new GetFxmlObjectCall(reference.source());
            case Include include -> null;
            default -> null;
        };
    }

    private void translateId(InstantiationElement instantiationElement) {
        if (instantiationElement.id() != null) {
            var instantiationExpression = expressionFor(instantiationElement);
            var id = instantiationElement.id();
            emit(new SetFxmlObjectCall(id, instantiationExpression));
            var clazz = classResolver.resolve(instantiationElement.className());
            reflectionHelper.getSetter(clazz, "id").ifPresent(setter ->
                emit(new MethodCall(new ExpressionTarget(instantiationExpression), setter.getName(), Literal.string(id)))
            );
        }
    }

    private Expression translateInstantiation(QName className, Instantiation instantiation) {
        var instanceVariable = gensym(className.className());
        var clazz = classResolver.resolve(className);
        QName resolvedClassName = QName.valueOf(clazz.getName());
        Expression instantiationExpression = switch (instantiation) {
            case Constructor _ when reflectionHelper.getNoArgsConstructor(clazz).isPresent() -> new ConstructorCall(resolvedClassName);
            case Constructor _ -> null;
            case Factory(String methodName) -> new MethodCall(new ClassTarget(resolvedClassName), methodName);
            case Value(String valueString) -> new MethodCall(new ClassTarget(resolvedClassName), "valueOf", Literal.string(valueString));
            case Constant(String constantName) -> new VariableExpression(QName.toString(resolvedClassName.packageName(), resolvedClassName.className(), constantName));
        };
        if (instantiationExpression != null) {
            emit(new VariableDeclaration(resolvedClassName, instanceVariable, instantiationExpression));
            return new VariableExpression(instanceVariable);
        } else {
            var constructor = reflectionHelper.getNamedArgsConstructor(getClass());
            if (constructor.isPresent()) {
                var namedArgs = reflectionHelper.getNamedConstructorArgs(constructor.get());
                // 
            }
        }
        return null;
    }

    private void translateBeanChildren(BeanElement bean, Iterable<? extends FxmlElement> fxmlElements) {
        Class<?> beanClass = classResolver.resolve(bean.beanType());
        Optional<String> defaultProperty = reflectionHelper.getDefaultProperty(beanClass);
        for (var child : fxmlElements) {
            switch (child) {
                case BeanProperty beanProperty ->
                    translatePropertyAccess(bean, beanClass, beanProperty);
                case InstanceElement instanceElement when defaultProperty.isPresent() ->
                    translatePropertyAccess(bean, beanClass, new PropertyElement(defaultProperty.get(), instanceElement));
                default -> translateFxml(child);
            }
        }
    }

    private record PropertyAccess(MethodTarget methodTarget, String methodName, Class<?> valueClass, Expression firstArgs) {
        PropertyAccess(MethodTarget methodTarget, String methodName, Class<?> valueClass) {
            this(methodTarget, methodName, valueClass, null);
        }
    }

    private void translatePropertyAccess(BeanElement bean, Class<?> beanClass, BeanProperty property) {
        MethodTarget beanTarget = new ExpressionTarget(expressionFor(bean));
        PropertyAccess propertyAccess = reflectionHelper.getSetter(beanClass, property.propertyName())
            .map(setter -> new PropertyAccess(beanTarget, setter.getName(), setter.getParameterTypes()[0]))
            .orElseGet(() -> reflectionHelper.getGetter(beanClass, property.propertyName())
                .filter(getter -> reflectionHelper.implementsList(getter.getReturnType()))
                .map(getter -> new PropertyAccess(new ExpressionTarget(new MethodCall(beanTarget, getter.getName())), "add", Object.class))
                .orElseGet(() -> reflectionHelper.implementsMap(beanClass)
                    ? new PropertyAccess(beanTarget, "put", Object.class, Literal.string(property.propertyName()))
                    : null
                )
            );
        List<Expression> valueExpressions = switch (property) {
            case PropertyElement propertyElement -> propertyElement.children().stream().map(this::translateFxml).toList();
            case PropertyValue propertyValue -> List.of(translateValueExpression(propertyValue.value(), propertyAccess.valueClass));
            case StaticProperty staticProperty -> throw new UnsupportedOperationException();
        };
        for (var valueExpression : valueExpressions) {
            List<Expression> methodArgs = propertyAccess.firstArgs != null ? List.of(propertyAccess.firstArgs, valueExpression) : List.of(valueExpression);
            emit(new MethodCall(propertyAccess.methodTarget, propertyAccess.methodName, methodArgs));
        }
    }

    private Expression translateValueExpression(ValueExpression valueExpression, Class<?> targetClass) {
        return switch (valueExpression) {
            case ValueExpression.String(String value) -> Literal.string(value);
            case ValueExpression.Reference(String source) -> new GetFxmlObjectCall(source);
            case ValueExpression.Binding value -> throw new UnsupportedOperationException();
            case ValueExpression.Location value -> throw new UnsupportedOperationException();
        };
    }

    public static void main(String[] args) throws Exception {
        var translation = FxmlTranslator.translateFxml("""
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
            """, FxmlTranslator.class.getClassLoader());
        System.out.println(JavaCode.statements2String(translation.statements()));
    }
}
