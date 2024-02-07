package no.hal.fxml.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JavaCode {

    public record Imports(Map<String, QName> imports) {}

    public sealed interface Statement
        permits ExecutableCall, VariableDeclaration {
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

    public sealed interface ExecutableCall extends Expression, Statement {
        public List<Expression> arguments();
    }

    public static String statements2String(List<Statement> statements) {
        return statements.stream().map(statement -> "%s;".formatted(statement)).collect(Collectors.joining("\n"));
    }

    public static String expressions2String(List<Expression> expressions) {
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
            return "%s%s(%s)".formatted(target, methodName, JavaCode.expressions2String(arguments));
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
            return "new %s(%s)".formatted(className, JavaCode.expressions2String(arguments));
        }
    }
}
