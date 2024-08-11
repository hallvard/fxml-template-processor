package no.hal.fxml.translator;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javafx.scene.Node;
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
import no.hal.fxml.model.JavaCode.Cast;
import no.hal.fxml.model.JavaCode.ClassDeclaration;
import no.hal.fxml.model.JavaCode.ClassTarget;
import no.hal.fxml.model.JavaCode.Comment;
import no.hal.fxml.model.JavaCode.ConstructorCall;
import no.hal.fxml.model.JavaCode.ConstructorDeclaration;
import no.hal.fxml.model.JavaCode.Expression;
import no.hal.fxml.model.JavaCode.ExpressionTarget;
import no.hal.fxml.model.JavaCode.FieldAssignment;
import no.hal.fxml.model.JavaCode.LambdaExpression;
import no.hal.fxml.model.JavaCode.LambdaMethodReference;
import no.hal.fxml.model.JavaCode.Literal;
import no.hal.fxml.model.JavaCode.Member;
import no.hal.fxml.model.JavaCode.MethodCall;
import no.hal.fxml.model.JavaCode.MethodDeclaration;
import no.hal.fxml.model.JavaCode.ObjectTarget;
import no.hal.fxml.model.JavaCode.Return;
import no.hal.fxml.model.JavaCode.Statement;
import no.hal.fxml.model.JavaCode.VariableDeclaration;
import no.hal.fxml.model.JavaCode.VariableExpression;
import no.hal.fxml.model.QName;
import no.hal.fxml.model.TypeRef;
import no.hal.fxml.model.ValueExpression;
import no.hal.fxml.parser.FxmlParser;
import no.hal.fxml.runtime.AbstractFxLoader;
import no.hal.fxml.runtime.FxLoader;
import no.hal.fxml.runtime.FxLoaderContext;

public class FxmlTranslator {
    
    public record Config(
        boolean includeCommentFxml,
        boolean useMethodReferences,
        boolean useCastObject
    ) {
        public Config() {
            this(true, false, false);
        }
    }

    private Config config;

    private final ClassResolver classResolver;
    private final ReflectionHelper reflectionHelper;

    public FxmlTranslator(Document fxmlDocument, QName targetClassName, ClassLoader classLoader, Config config) {
        this.config = config;
        this.classResolver = new ClassResolver(classLoader, fxmlDocument.imports());
        this.reflectionHelper = new ReflectionHelper();
    }

    private QName rootType = null;

    private List<Statement> builderStatements = new ArrayList<>();
    private Map<FxmlElement, Expression> expressions = new HashMap<>();

    private void emitBuilderStatement(Statement statement) {
        builderStatements.add(statement);
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

    private static final String FX_LOADER_CONTEXT_VARIABLE = "fxLoaderContext";

    public static ClassDeclaration translateFxml(Document fxmlDocument, QName targetClassName, ClassLoader classLoader, Config config) {
        FxmlTranslator translator = new FxmlTranslator(fxmlDocument, targetClassName, classLoader, config);
        FxmlElement rootElement = fxmlDocument.instanceElement();
        Expression rootExpression = translator.fxml2BuilderStatements(rootElement);
        translator.emitBuilderStatement(new Return(rootExpression));
        List<Member> members = new ArrayList<>();
        members.add(new ConstructorDeclaration("public", targetClassName.className(), List.of()));
        members.add(new ConstructorDeclaration("public", targetClassName.className(), List.of(
            VariableDeclaration.parameter(TypeRef.valueOf("java.util.Map<String, Object>"), "namespace")
        )));
        members.add(new MethodDeclaration("protected", "build", new TypeRef(translator.rootType), List.of(
                VariableDeclaration.parameter(TypeRef.of(FxLoaderContext.class), FX_LOADER_CONTEXT_VARIABLE)
            ), translator.builderStatements)
        );
        if (fxmlDocument.controllerClassName() != null) {
            QName controllerClassName = fxmlDocument.controllerClassName();
            members.add(new MethodDeclaration("protected", "createController", new TypeRef(controllerClassName), null, List.of(
                new Return(new ConstructorCall(controllerClassName))
            )));
            QName controllerHelperClassName = new QName(controllerClassName.packageName(), controllerClassName.className() + "Helper");
            members.add(new VariableDeclaration("private", new TypeRef(controllerHelperClassName), "controllerHelper", null));
            members.add(new MethodDeclaration("protected", "initializeController", null, null, List.of(
                new FieldAssignment(ObjectTarget.thisTarget(), "controllerHelper", new ConstructorCall(controllerHelperClassName, List.of(
                    new MethodCall(ObjectTarget.thisTarget(), "getNamespace"),
                    castObject(new TypeRef(controllerClassName), new VariableExpression("this.controller"), config)
                ))),
                new MethodCall(new ExpressionTarget("this.controllerHelper"), "initializeController")
            )));
        }
        return new ClassDeclaration(targetClassName,
                new TypeRef(QName.of(AbstractFxLoader.class),
                    translator.rootType != null ? new TypeRef(translator.rootType) : TypeRef.of(Node.class),
                    new TypeRef(fxmlDocument.controllerClassName() != null ? fxmlDocument.controllerClassName() : QName.of(Object.class))
                ),
                null,  members
        );
    }
    public static ClassDeclaration translateFxml(String fxml, QName targetClassName, ClassLoader classLoader, Config config) throws Exception {
        return translateFxml(FxmlParser.parseFxml(fxml), targetClassName, classLoader, config);
    }
    public static ClassDeclaration translateFxml(InputStream input, QName targetClassName, ClassLoader classLoader, Config config) throws Exception {
        return translateFxml(FxmlParser.parseFxml(input), targetClassName, classLoader, config);
    }

    private Expression fxml2BuilderStatements(FxmlElement fxmlElement) {
        if (config.includeCommentFxml) {
            emitBuilderStatement(Comment.line(fxmlElement.toShortString()));
        }
        return switch (fxmlElement) {
            case Define define -> {
                define.children().forEach(this::fxml2BuilderStatements);
                yield null;
            }
            case Root root -> {
                if (this.rootType == null) {
                    var rootClass = classResolver.resolve(root.typeName());
                    this.rootType = QName.of(rootClass);
                }
                var rootVariable = gensym("root");
                emitBuilderStatement(new VariableDeclaration(root.typeName(), rootVariable, new VariableExpression("_fxmlLoader.getRoot()")));
                var rootExpression = expressionFor(root, new VariableExpression(rootVariable));
                translateBeanChildren(root, root.children());
                yield rootExpression;
            }
            case InstantiationElement instantiationElement -> {
                var instanceClass = classResolver.resolve(instantiationElement.className());
                Objects.requireNonNull(instanceClass, "Couldn't resolve class for instantiation: " + instantiationElement.className());
                if (this.rootType == null) {
                    this.rootType = QName.of(instanceClass);
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
                        instantiationExpression = new ConstructorCall(QName.of(instanceClass), argNames.stream().map(argExpressions::get).toList());
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
            case Reference reference -> getFxmlObjectCall(reference.source());

            case Include(String id, String source) -> {
                var loaderVar = gensym("fxIncludeLoader");
                emitBuilderStatement(new VariableDeclaration(TypeRef.of(FxLoader.class, null, null), loaderVar, new MethodCall(FX_LOADER_CONTEXT_VARIABLE, "loadFxml", Literal.string(source))));
                var rootVar = gensym("fxIncludeRoot");
                emitBuilderStatement(new VariableDeclaration(TypeRef.of(Node.class), rootVar, new MethodCall(loaderVar, "getRoot")));
                var rootVarExpression = new VariableExpression(rootVar);
                emitBuilderStatement(setFxmlObjectCall(id, rootVarExpression));
                emitBuilderStatement(setFxmlObjectCall(id + "Controller", new MethodCall(loaderVar, "getController")));
                yield rootVarExpression;
            }
            default -> null;
        };
    }

    static MethodCall getFxmlObjectCall(String id) {
        return new MethodCall(ObjectTarget.thisTarget(), "getFxmlObject", Literal.string(id));
    }
    static MethodCall setFxmlObjectCall(String id, Expression expression) {
        return new MethodCall(ObjectTarget.thisTarget(), "setFxmlObject", List.of(Literal.string(id), expression));
    }
    static MethodCall setFxmlObjectCall(String id, String variableName) {
        return new MethodCall(ObjectTarget.thisTarget(), "setFxmlObject", List.of(Literal.string(id), new VariableExpression(variableName)));
    }

    private Map<String, Expression> idMap = new HashMap<>();

    private void translateId(InstantiationElement instantiationElement) {
        if (instantiationElement.id() != null) {
            var instantiationExpression = expressionFor(instantiationElement);
            var id = instantiationElement.id();
            idMap.put(id, instantiationExpression);
            emitBuilderStatement(setFxmlObjectCall(id, instantiationExpression));
            var clazz = classResolver.resolve(instantiationElement.className());
            reflectionHelper.getSetter(clazz, "id").ifPresent(setter ->
                emitBuilderStatement(new MethodCall(new ExpressionTarget(instantiationExpression), setter.getName(), Literal.string(id)))
            );
        }
    }

    private Expression translateInstantiation(Class<?> clazz, Instantiation instantiation) {
        QName resolvedClassName = QName.of(clazz);
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

    private static Expression castObject(TypeRef type, Expression expr, Config config) {
        return config.useCastObject ? new Cast(type, expr) : expr;
    }
    private Expression castObject(TypeRef type, Expression expr) {
        return FxmlTranslator.castObject(type, expr, config);
    }

    private Expression translateValueExpression(ValueExpression valueExpression, Class<?> targetClass) {
        return switch (valueExpression) {
            case ValueExpression.String(String value) -> new Literal(value, targetClass);
            case ValueExpression.IdReference(String source) -> castObject(TypeRef.of(targetClass), getFxmlObjectCall(source));
            case ValueExpression.Binding value -> throw new UnsupportedOperationException();
            case ValueExpression.Location value -> throw new UnsupportedOperationException();
            case ValueExpression.MethodReference(String methodName) -> {
                var target = new ExpressionTarget("this.controllerHelper");
                yield config.useMethodReferences()
                ? new LambdaMethodReference(target, methodName)
                : new LambdaExpression(List.of("event"), new MethodCall(target, methodName, new VariableExpression("event")))
                ;
            }
        };
    }

    //

    private static final String FXML_SAMPLE = """
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
        """;

    public static void main(String[] args) throws Exception {
        Config config = new Config();
        if (args.length == 0) {
            Document fxmlDoc = FxmlParser.parseFxml(FXML_SAMPLE);
            var classDeclaration = FxmlTranslator.translateFxml(fxmlDoc, QName.valueOf("no.hal.fxml.translator.TestOutput"), FxmlTranslator.class.getClassLoader(), config);
            System.out.println(JavaCode.toJavaSource(classDeclaration));
        } else {
            Path source = Path.of(args[0]);
            Path target = (args.length >= 2 ? Path.of(args[1]) : source);
            if (Files.isDirectory(source)) {
                Files.find(source, Integer.MAX_VALUE, (path, attributes) -> path.getFileName().toString().endsWith(".fxml"))
                    .forEach(path -> {
                        try {
                            translateFile(source, path, target);
                        } catch (Exception ex) {
                            // ignore
                        }
                });
            } else {
                translateFile(Path.of("."), source, target);
            }
        }
    }

    public record FxmlTranslation(Path path, ClassDeclaration builderClass) {
    }

    public static FxmlTranslation translateFile(Path root, Path path, Path outputFolder) throws Exception {
        Document fxmlDoc = FxmlParser.parseFxml(path.toFile());
        try {
            Config config = new Config();
            Path resourcePath = root.relativize(path);
            QName className = QName.valueOf(resourcePath.toString().replace(".fxml", "Loader").replace('/', '.'));
            var classDeclaration = FxmlTranslator.translateFxml(fxmlDoc, className, FxmlTranslator.class.getClassLoader(), config);
            var javaSource = JavaCode.toJavaSource(classDeclaration, "// generated from %s".formatted(resourcePath));
            var javaPath = outputFolder.resolve(className.toString().replace(".", "/") + ".java");
            Files.write(javaPath, javaSource.getBytes());
            return new FxmlTranslation(Path.of("/" + resourcePath.toString()), classDeclaration);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }
}
