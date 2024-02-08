package no.hal.fxml.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class JavaCode {

    public record Imports(Map<String, QName> imports) {

        public Imports {
            imports = new HashMap<>(imports);
        }

        public boolean imports(QName qName) {
            return "java.lang".equals(qName.packageName()) || qName.equals(imports.get(qName.className()));
        }

        public boolean importIfAvailable(QName qName) {
            String key = qName.className();
            if (imports.containsKey(key)) {
                return qName.equals(imports.get(key));
            } else {
                imports.put(key, qName);
                return true;
            }
        }
    }

    public sealed interface Statement
        permits VariableDeclaration, Return, ExecutableCall {
    }

    public record VariableDeclaration(QName className, String variableName, Expression expression)
        implements Statement {
        public VariableDeclaration(String className, String variableName, Expression expression) {
            this(QName.valueOf(className), variableName, expression);
        }
        public static VariableDeclaration instantiation(String className, String variableName) {
            return new VariableDeclaration(className, variableName, new ConstructorCall(className));
        }
        @Override
        public String toString() {
            return "%s %s = %s".formatted(className != null ? className : "var", variableName, expression);
        }
    }

    public record Return(Expression expression) implements Statement {
        public Return(String variableName) {
            this(new VariableExpression(variableName));
        }
        @Override
        public String toString() {
            return "return %s".formatted(expression);
        }
    }

    public sealed interface Expression
        permits VariableExpression, Literal, ExecutableCall {
    }

    public record VariableExpression(String variableName)
        implements Expression {
        @Override
        public String toString() {
            return variableName;
        }
    }

    public record Literal(String literal, Class<?> clazz)
        implements Expression {
        public static Literal string(String literal) {
            return new Literal(literal, String.class);
        }
        @Override
        public String toString() {
            return clazz == String.class ? "\"%s\"".formatted(literal) : literal;
        }
    }

    public sealed interface MethodTarget
        permits ClassTarget, ExpressionTarget {
    }

    public record ClassTarget(QName className) implements MethodTarget {
        public ClassTarget(String className) {
            this(QName.valueOf(className));
        }
        @Override
        public String toString() {
            return "%s.".formatted(className);
        }
    }

    public record ExpressionTarget(Expression expression) implements MethodTarget {
        public ExpressionTarget(String variableName) {
            this(new VariableExpression(variableName));
        }
        @Override
        public String toString() {
            return "%s.".formatted(expression);
        }
    }

    public sealed interface ExecutableCall extends Expression, Statement
        permits MethodCall, GetFxmlObjectCall, SetFxmlObjectCall, ConstructorCall {
        public List<Expression> arguments();
    }

    public static String statements2String(List<Statement> statements) {
        return statements.stream().map(statement -> "%s;".formatted(statement)).collect(Collectors.joining("\n"));
    }
    
    public static String expression2String(Expression expression) {
        return expression.toString();
    } 
    public static String expressionList2String(List<Expression> expressions) {
        return expressions.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    public record MethodCall(MethodTarget target, String methodName, List<Expression> arguments)
        implements ExecutableCall {
        public MethodCall(MethodTarget target, String methodName, Expression singleArg) {
            this(target, methodName, Collections.singletonList(singleArg));
        }
        public MethodCall(Expression expression, String methodName, Expression singleArg) {
            this(new ExpressionTarget(expression), methodName, singleArg);
        }
        public MethodCall(String variableName, String methodName, Expression singleArg) {
            this(new ExpressionTarget(variableName), methodName, singleArg);
        }
        public MethodCall(MethodTarget target, String methodName) {
            this(target, methodName, Collections.emptyList());
        }
        public MethodCall(Expression expression, String methodName) {
            this(new ExpressionTarget(expression), methodName);
        }
        public MethodCall(String variableName, String methodName) {
            this(new ExpressionTarget(variableName), methodName);
        }
        @Override
        public String toString() {
            return "%s%s(%s)".formatted(target, methodName, JavaCode.expressionList2String(arguments));
        }
    }

    public record GetFxmlObjectCall(List<Expression> arguments) implements ExecutableCall {
        public GetFxmlObjectCall(String id) {
            this(List.of(Literal.string(id)));
        }
        @Override
        public String toString() {
            return "getFxmlObject(%s)".formatted(JavaCode.expressionList2String(arguments));
        }
    }
    public record SetFxmlObjectCall(List<Expression> arguments) implements ExecutableCall {
        public SetFxmlObjectCall(String id, Expression expression) {
            this(List.of(Literal.string(id), expression));
        }
        public SetFxmlObjectCall(String id, String variableName) {
            this(List.of(Literal.string(id), new VariableExpression(variableName)));
        }
        @Override
        public String toString() {
            return "setFxmlObject(%s)".formatted(JavaCode.expressionList2String(arguments));
        }
    }

    public record ConstructorCall(QName className, List<Expression> arguments)
        implements ExecutableCall {
        public ConstructorCall(QName className) {
            this(className, Collections.emptyList());
        }
        public ConstructorCall(String className) {
            this(QName.valueOf(className));
        }
        @Override
        public String toString() {
            return "new %s(%s)".formatted(className, JavaCode.expressionList2String(arguments));
        }
    }

    //

    public record Formatter(Imports imports) {

        public <T> String formatWith(T t, BiConsumer<T, StringBuilder> formatter) {
            StringBuilder builder = new StringBuilder();
            formatter.accept(t, builder);
            return builder.toString();
        }

        public String format(List<Statement> statements) {
            return formatWith(statements, this::format);
        }
        public String format(Statement statement) {
            return formatWith(statement, this::format);
        }
        public String format(Expression expression) {
            return formatWith(expression, this::format);
        }

        public void format(List<Statement> statements, StringBuilder builder) {
            statements.forEach(statement -> format(statement, builder));
        }

        public String toString(QName className) {
            if (imports.importIfAvailable(className)) {
                return className.className();
            } else {
                return className.toString();
            }
        }

        public void format(QName className, StringBuilder builder) {
            builder.append(toString(className));
        }

        public void format(Statement statement, StringBuilder builder) {
            switch (statement) {
                case VariableDeclaration varDecl -> {
                    format(varDecl.className(), builder);
                    builder.append(" ");
                    builder.append(varDecl.variableName());
                    builder.append(" = ");
                    format(varDecl.expression(), builder);
                }
                case Return ret -> {
                    builder.append("return ");
                    format(ret.expression(), builder);
                }
                case ExecutableCall call -> {
                    format((Expression) call, builder);
                }
            }
            builder.append(";\n");
        }

        public void formatArgumentList(List<Expression> arguments, StringBuilder builder) {
            builder.append("(");
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                format(arguments.get(i), builder);
            }
            builder.append(")");
        }

        public void format(Expression expression, StringBuilder builder) {
            switch (expression) {
                case VariableExpression(String variableName) -> builder.append(variableName);
                case Literal(String literal, Class<?> clazz) -> {
                    if (String.class.equals(clazz)) {
                        builder.append("\"");
                        builder.append(literal);
                        builder.append("\"");
                    } else {
                        builder.append(literal);
                    }
                }
                case MethodCall(MethodTarget target, String methodName, List<Expression> arguments) -> {
                    format(target, builder);
                    builder.append(".");
                    builder.append(methodName);
                    formatArgumentList(arguments, builder);
                }
                case GetFxmlObjectCall(List<Expression> arguments) -> {
                    builder.append("getFxmlObject");
                    formatArgumentList(arguments, builder);
                }
                case SetFxmlObjectCall(List<Expression> arguments) -> {
                    builder.append("setFxmlObject");
                    formatArgumentList(arguments, builder);
                }
                case ConstructorCall(QName className, List<Expression> arguments) -> {
                    builder.append("new ");
                    format(className, builder);
                    formatArgumentList(arguments, builder);
                }
            }
        }

        public void format(MethodTarget methodTarget, StringBuilder builder) {
            switch (methodTarget) {
                case ClassTarget(QName className) -> {
                    format(className, builder);
                }
                case ExpressionTarget(Expression expr) -> {
                    format(expr, builder);
                }
            }
        }

        public String format(Imports imports) {
            return formatWith(imports, this::format);
        }

        public void format(Imports imports, StringBuilder builder) {
            imports.imports.values().forEach(className -> {
                builder.append("import ");
                builder.append(className);
                builder.append(";\n");
            });
        }
    }

    public static Optional<QName> findVariableType(String variableName, List<Statement> statements) {
        return statements.stream()
            .filter(statement -> statement instanceof VariableDeclaration varDecl && variableName.equals(varDecl.variableName()))
            .map(statement -> ((VariableDeclaration) statement).className())
            .findFirst();
    }
}
