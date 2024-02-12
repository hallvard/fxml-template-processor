package no.hal.fxml.translator;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
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
import no.hal.fxml.model.JavaCode.ClassTarget;
import no.hal.fxml.model.JavaCode.ConstructorCall;
import no.hal.fxml.model.JavaCode.Expression;
import no.hal.fxml.model.JavaCode.ExpressionTarget;
import no.hal.fxml.model.JavaCode.FieldAssignment;
import no.hal.fxml.model.JavaCode.GetFxmlObjectCall;
import no.hal.fxml.model.JavaCode.LambdaExpression;
import no.hal.fxml.model.JavaCode.Literal;
import no.hal.fxml.model.JavaCode.MethodCall;
import no.hal.fxml.model.JavaCode.MethodDeclaration;
import no.hal.fxml.model.JavaCode.ObjectTarget;
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

    private QName rootType = null;

    private List<Statement> builderStatements = new ArrayList<>();
    private List<Statement> initializerStatements = new ArrayList<>();
    private Map<FxmlElement, Expression> expressions = new HashMap<>();
    private Map<String, List<Expression>> methodReferences = new HashMap<>();

    private void emitBuilderStatement(Statement statement) {
        builderStatements.add(statement);
    }
    private void emitInitializerStatement(Statement statement) {
        initializerStatements.add(statement);
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

    public record FxmlTranslation(MethodDeclaration builder, MethodDeclaration initializer, List<MethodDeclaration> extraMethodds) {
    }

    public static FxmlTranslation translateFxml(Document fxmlDocument, ClassLoader classLoader) {
        FxmlTranslator translator = new FxmlTranslator(fxmlDocument, classLoader);
        FxmlElement rootElement = fxmlDocument.instanceElement();
        Expression rootExpression = translator.fxml2BuilderStatements(rootElement);
        translator.emitBuilderStatement(new Return(rootExpression));
        MethodDeclaration initializerMethod = null;
        List<MethodDeclaration> extraMethods = null;
        if (fxmlDocument.controllerClassName() != null) {
            Class<?> controllerClass = translator.classResolver.resolve(fxmlDocument.controllerClassName());
            translator.fxml2InitializerStatements(controllerClass);
            initializerMethod = new MethodDeclaration("initialize", null, List.of(new VariableDeclaration(fxmlDocument.controllerClassName(), "controller", null)), translator.initializerStatements);
            extraMethods = translator.methodReferences.entrySet().stream()
                .map(entry -> translator.bridgeMethodDeclaration(controllerClass, entry.getKey(), entry.getValue()))
                .toList();
        }
        return new FxmlTranslation(
            new MethodDeclaration("build", translator.rootType, List.of(), translator.builderStatements),
            initializerMethod,
            extraMethods
        );
    }
    public static FxmlTranslation translateFxml(String fxml, ClassLoader classLoader) throws Exception {
        return translateFxml(FxmlParser.parseFxml(fxml), classLoader);
    }
    public static FxmlTranslation translateFxml(InputStream input, ClassLoader classLoader) throws Exception {
        return translateFxml(FxmlParser.parseFxml(input), classLoader);
    }

    private Expression fxml2BuilderStatements(FxmlElement fxmlElement) {
        return switch (fxmlElement) {
            case Define define -> {
                define.children().forEach(this::fxml2BuilderStatements);
                yield null;
            }
            case Root root -> {
                if (this.rootType == null) {
                    var rootClass = classResolver.resolve(root.typeName());
                    this.rootType = QName.valueOf(rootClass.getName());
                }
                var rootVariable = gensym("root");
                emitBuilderStatement(new VariableDeclaration(root.typeName(), rootVariable, new VariableExpression("_fxmlLoader.getRoot()")));
                var rootExpression = expressionFor(root, new VariableExpression(rootVariable));
                translateBeanChildren(root, root.children());
                yield rootExpression;
            }
            case InstantiationElement instantiationElement -> {
                var instanceClass = classResolver.resolve(instantiationElement.className());
                if (this.rootType == null) {
                    this.rootType = QName.valueOf(instanceClass.getName());
                }
                var instantiationExpression = translateInstantiation(instanceClass, instantiationElement.instantiation());
                var remainingBeanChildren = new ArrayList<>(instantiationElement.children());
                if (instantiationExpression == null) {
                    var constructor = reflectionHelper.getNamedArgsConstructor(instanceClass);
                    if (constructor.isPresent()) {
                        // try constructor with @NamedArg, e.g. Color(red, green, blue))
                        var namedArgs = reflectionHelper.getNamedConstructorArgs(constructor.get());
                        var argNames = namedArgs.keySet();
                        Map<String, Expression> argExpressions = new HashMap<>();
                        // prefill with default values, that may be overwritten
                        for (var namedArg : namedArgs.values()) {
                            if (! namedArg.defaultValue().isBlank()) {
                                Expression defaultExpr = translateValueExpression(new ValueExpression.String(namedArg.defaultValue()), namedArg.type());
                                argExpressions.put(namedArg.name(), defaultExpr);
                            }
                        }
                        // translate properties corresponding to named arguments
                        for (var child : instantiationElement.children()) {
                            if (child instanceof BeanProperty beanProperty && argNames.contains(beanProperty.propertyName())) {
                                var namedArgInfo = namedArgs.get(beanProperty.propertyName());
                                List<Expression> expressions = translatePropertyValues(beanProperty, namedArgInfo.type());
                                if (expressions.size() != 1) {
                                    throw new IllegalArgumentException("Property should only have one value: " + beanProperty);
                                }
                                argExpressions.put(beanProperty.propertyName(), expressions.getFirst());
                                remainingBeanChildren.remove(child);
                            }
                        }
                        if (namedArgs.size() > argExpressions.size()) {
                            var missingProperties = new ArrayList<>(argNames);
                            missingProperties.removeAll(argExpressions.keySet());
                            throw new IllegalArgumentException("Missing properties: " + missingProperties);
                        }
                        instantiationExpression = new ConstructorCall(QName.valueOf(instanceClass.getName()), argNames.stream().map(argExpressions::get).toList());
                    }
                    // how to support other JavaFXBuilderFactory logic???
                }
                if (instantiationExpression == null) {
                    throw new IllegalArgumentException("Couldn't create instance for " + instantiationElement);
                }
                var instanceVariable = gensym(instanceClass.getSimpleName());
                emitBuilderStatement(new VariableDeclaration(instanceClass.getName(), instanceVariable, instantiationExpression));
                var instanceExpression = new VariableExpression(instanceVariable);
                expressionFor(instantiationElement, instanceExpression);
                translateId(instantiationElement);
                translateBeanChildren(instantiationElement, remainingBeanChildren);
                yield instanceExpression;
            }
            case Reference reference -> new GetFxmlObjectCall(reference.source());
            case Include include -> null;
            default -> null;
        };
    }

    private Map<String, Expression> idMap = new HashMap<>();

    private void translateId(InstantiationElement instantiationElement) {
        if (instantiationElement.id() != null) {
            var instantiationExpression = expressionFor(instantiationElement);
            var id = instantiationElement.id();
            idMap.put(id, instantiationExpression);
            emitBuilderStatement(new SetFxmlObjectCall(id, instantiationExpression));
            var clazz = classResolver.resolve(instantiationElement.className());
            reflectionHelper.getSetter(clazz, "id").ifPresent(setter ->
                emitBuilderStatement(new MethodCall(new ExpressionTarget(instantiationExpression), setter.getName(), Literal.string(id)))
            );
        }
    }

    private Expression translateInstantiation(Class<?> clazz, Instantiation instantiation) {
        QName resolvedClassName = QName.valueOf(clazz.getName());
        return switch (instantiation) {
            case Constructor _ when reflectionHelper.getNoArgsConstructor(clazz).isPresent() -> new ConstructorCall(resolvedClassName);
            case Constructor _ -> null;
            case Factory(String methodName) -> new MethodCall(new ClassTarget(resolvedClassName), methodName);
            case Value(String valueString) -> new MethodCall(new ClassTarget(resolvedClassName), "valueOf", Literal.string(valueString));
            case Constant(String constantName) -> new VariableExpression(QName.toString(resolvedClassName.packageName(), resolvedClassName.className(), constantName));
        };
    }

    private void translateBeanChildren(BeanElement bean, Iterable<? extends FxmlElement> fxmlElements) {
        Class<?> beanClass = classResolver.resolve(bean.beanType());
        Optional<String> defaultProperty = reflectionHelper.getDefaultProperty(beanClass);
        for (var child : fxmlElements) {
            switch (child) {
                case BeanProperty beanProperty -> 
                    translatePropertyAccess(bean, beanClass, beanProperty);
                case InstanceElement instanceElement ->
                    translatePropertyAccess(bean, beanClass, new PropertyElement(defaultProperty.get(), instanceElement));
                default -> fxml2BuilderStatements(child);
            }
        }
    }

    private record PropertyAccess(ObjectTarget methodTarget, String methodName, Class<?> valueClass, Expression firstArgs) {
        PropertyAccess(ObjectTarget methodTarget, String methodName, Class<?> valueClass) {
            this(methodTarget, methodName, valueClass, null);
        }
    }

    private void translatePropertyAccess(BeanElement bean, Class<?> beanClass, BeanProperty property) {
        ObjectTarget beanTarget = new ExpressionTarget(expressionFor(bean));
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
        if (propertyAccess == null) {
            throw new IllegalArgumentException("No property access for " + property.propertyName() + " of " + beanClass);
        }
        List<Expression> valueExpressions = translatePropertyValues(property, propertyAccess.valueClass);
        for (var valueExpression : valueExpressions) {
            List<Expression> methodArgs = propertyAccess.firstArgs != null ? List.of(propertyAccess.firstArgs, valueExpression) : List.of(valueExpression);
            emitBuilderStatement(new MethodCall(propertyAccess.methodTarget, propertyAccess.methodName, methodArgs));
        }
    }

    private List<Expression> translatePropertyValues(BeanProperty property, Class<?> valueClass) {
        return switch (property) {
            case PropertyElement propertyElement -> propertyElement.children().stream().map(this::fxml2BuilderStatements).toList();
            case PropertyValue propertyValue -> List.of(translateValueExpression(propertyValue.value(), valueClass));
            case StaticProperty staticProperty -> throw new UnsupportedOperationException();
        };
    }

    private String bridgeMethodFormat = "hash_%s";

    private Expression translateValueExpression(ValueExpression valueExpression, Class<?> targetClass) {
        return switch (valueExpression) {
            case ValueExpression.String(String value) -> new Literal(value, targetClass);
            case ValueExpression.IdReference(String source) -> new GetFxmlObjectCall(source);
            case ValueExpression.Binding value -> throw new UnsupportedOperationException();
            case ValueExpression.Location value -> throw new UnsupportedOperationException();
            case ValueExpression.MethodReference(String methodName) -> {
                List<Expression> argList = new ArrayList<>();
                // argList will be updated, according to argument list of controller method
                methodReferences.put(methodName, argList);
                yield new LambdaExpression("event", new MethodCall((ObjectTarget) null, bridgeMethodFormat.formatted(methodName), argList));
            }
        };
    }

    //

    private void fxml2InitializerStatements(Class<?> controllerClass) {
        var fxmlAnnotatedMembers = reflectionHelper.findAnnotatedMembers(controllerClass, FXML.class);
        for (var member : fxmlAnnotatedMembers.keySet()) {
            switch (member) {
                case Field field -> {
                    var fieldAssignment = new FieldAssignment("controller", member.getName(), new GetFxmlObjectCall(member.getName()));
                    emitInitializerStatement(fieldAssignment);
                }
                case Method method -> {
                    String propertyName = reflectionHelper.propertyName("set", method.getName());
                    if (idMap.containsKey(propertyName)) {
                        var methodCall = new MethodCall("controller", member.getName(), new GetFxmlObjectCall(propertyName));
                        emitInitializerStatement(methodCall);
                    }
                }
                default -> {}
            }
        }
        fxmlAnnotatedMembers.keySet().stream()
            .filter(member -> member instanceof Method && "initialize".equals(member.getName()))
            .forEach(method -> emitInitializerStatement(new MethodCall("controller", method.getName())));
    }

    private MethodDeclaration bridgeMethodDeclaration(Class<?> controllerClass, String methodName, List<Expression> argList) {
        Method method = reflectionHelper.getMethod(controllerClass, methodName, Event.class);
        if (method == null) {
            method = reflectionHelper.getMethod(controllerClass, methodName);
        }
        if (method == null) {
            throw new IllegalStateException("No method in " + controllerClass + " for method reference #" + methodName);
        }
        List<VariableDeclaration> paramList = new ArrayList<>();
        if (method.getParameterCount() == 1) {
            paramList.add(new VariableDeclaration(QName.valueOf(method.getParameterTypes()[0].getName()), "event", null));
            argList.add(new VariableExpression("event"));
        }
        MethodDeclaration methodDeclaration = new MethodDeclaration(bridgeMethodFormat.formatted(methodName), null, paramList, List.of(
            new MethodCall(new ExpressionTarget("controller"), methodName, argList)
        ));
        return methodDeclaration;
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
        System.out.println(translation.builder);
        System.out.println(translation.initializer);
    }
}
