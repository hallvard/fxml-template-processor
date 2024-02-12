package no.hal.fxml.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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

    public sealed interface Member permits MethodDeclaration, VariableDeclaration {
        public String getName();
    }

    public record MethodDeclaration(String modifiers, String methodName, QName returnType, List<VariableDeclaration> parameters, List<Statement> body)
        implements Member {
        @Override
        public String toString() {
            String returnString = returnType != null ? returnType.toString() : "void";
            return (body != null
                ? "%s %s %s(%s) {\n%s\n}".formatted(modifiers, returnString, methodName, list2String(parameters), statements2String(body))
                : "%s %s %s(%s)".formatted(modifiers, returnString, methodName, list2String(parameters))
            );
        }
        @Override
        public String getName() {
            return methodName;
        }
    }

    public sealed interface Statement
        permits VariableDeclaration, Return, ExecutableCall, FieldAssignment {
    }

    public record VariableDeclaration(QName className, String variableName, Expression expression)
        implements Statement, Member {
        public VariableDeclaration(String className, String variableName, Expression expression) {
            this(QName.valueOf(className), variableName, expression);
        }
        public static VariableDeclaration instantiation(String className, String variableName) {
            return new VariableDeclaration(className, variableName, new ConstructorCall(className));
        }
        @Override
        public String toString() {
            return (expression != null 
                ? "%s %s = %s".formatted(className != null ? className : "var", variableName, expression)
                :  "%s %s".formatted(className != null ? className : "var", variableName)
            );
        }
        @Override
        public String getName() {
            return variableName;
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
        permits VariableExpression, Literal, ExecutableCall, LambdaExpression, FieldAssignment {
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
            if (clazz == String.class) {
                return "\"%s\"".formatted(literal);
            } else if (clazz.isEnum()) {
                return "%s.%s".formatted(clazz.getName(), literal);
            } else {
                return "(%s)%s".formatted(clazz.getName(), literal);
            }
        }
    }

    public sealed interface ObjectTarget
        permits ClassTarget, ExpressionTarget {
    }

    public record ClassTarget(QName className) implements ObjectTarget {
        public ClassTarget(String className) {
            this(QName.valueOf(className));
        }
        @Override
        public String toString() {
            return "%s.".formatted(className);
        }
    }

    public record ExpressionTarget(Expression expression) implements ObjectTarget {
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
    public static <T> String list2String(List<T> items) {
        return items.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    public record MethodCall(ObjectTarget target, String methodName, List<Expression> arguments)
        implements ExecutableCall {
        public MethodCall(ObjectTarget target, String methodName, Expression singleArg) {
            this(target, methodName, Collections.singletonList(singleArg));
        }
        public MethodCall(Expression expression, String methodName, Expression singleArg) {
            this(new ExpressionTarget(expression), methodName, singleArg);
        }
        public MethodCall(String variableName, String methodName, Expression singleArg) {
            this(new ExpressionTarget(variableName), methodName, singleArg);
        }
        public MethodCall(ObjectTarget target, String methodName) {
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
            return "%s%s(%s)".formatted(target != null ? target : "", methodName, JavaCode.list2String(arguments));
        }
    }

    public record LambdaExpression(List<String> paramList, Expression expression)
        implements Expression {
        public LambdaExpression(String param, Expression expression) {
            this(List.of(param), expression);
        }
        @Override
        public String toString() {
            return "(%s) -> %s".formatted(JavaCode.list2String(paramList), expression);
        }
    }

    public record GetFxmlObjectCall(List<Expression> arguments) implements ExecutableCall {
        public GetFxmlObjectCall(String id) {
            this(List.of(Literal.string(id)));
        }
        @Override
        public String toString() {
            return "getFxmlObject(%s)".formatted(JavaCode.list2String(arguments));
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
            return "setFxmlObject(%s)".formatted(JavaCode.list2String(arguments));
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
            return "new %s(%s)".formatted(className, JavaCode.list2String(arguments));
        }
    }

    public record FieldAssignment(ObjectTarget target, String fieldName, Expression valuExpression)
        implements Expression, Statement {
        public FieldAssignment(String variableName, String fieldName, Expression valuExpression) {
            this(new ExpressionTarget(variableName), fieldName, valuExpression);
        }
        @Override
        public String toString() {
            return "%s%s = %s".formatted(target, fieldName, valuExpression);
        }
    }

    //

    public static class Formatter {

        private Imports imports;

        public Formatter(Imports imports) {
            this.imports = imports;
        }

        private StringBuilder builder = new StringBuilder();
        private int indentLevel = 0;
        private String indentString = "   ";

        public static <T> String format(Imports imports, T t, BiConsumer<Formatter, T> format) {
            Formatter formatter = new Formatter(imports);
            format.accept(formatter, t);
            return formatter.builder.toString();
        }

        public <T> String format(T t, BiConsumer<Formatter, T> format) {
            return format(this.imports, t, format);
        }

        //

        @Override
        public String toString() {
            return builder.toString();
        }

        public boolean append(String s) {
            if (s != null) {
                builder.append(s);
                return s.length() > 0;
            }
            return false;
        }

        public void indent() {
            for (int i = 0; i < indentLevel; i++) {
                append(indentString);
            }
        }
        public void newlines(int i) {
            while (i-- > 0) {
                append("\n");
            }
        }
        public void newline() {
            newlines(1);
        }

        public <T> void withIndentation(int d, T t, BiConsumer<Formatter, T> formatter) {
            indentLevel += d;
            formatter.accept(this, t);
            indentLevel -= d;
        }
        public <T> void withIndentation(T t, BiConsumer<Formatter, T> formatter) {
            withIndentation(1, t, formatter);
        }

        public void format(MethodDeclaration method) {
            indent();
            if (append(method.modifiers())) {
                append(" ");
            }
            if (method.returnType() != null) {
                format(method.returnType());
            } else {
                append("void");
            }
            append(" ");
            append(method.methodName());
            formatList("(", method.parameters(), ") {\n", this::format);
            withIndentation(method.body(), Formatter::format);
            indent();
            append("}\n");
        }

        public void format(List<Statement> statements) {
            statements.forEach(this::format);
        }

        public String toString(QName className) {
            if (imports.importIfAvailable(className)) {
                return className.className();
            } else {
                return className.toString();
            }
        }

        public void format(QName className) {
            append(toString(className));
        }
        public void format(Class<?> clazz) {
            format(QName.valueOf(clazz.getName()));
        }

        public void format(VariableDeclaration varDecl) {
            format(varDecl.className());
            append(" ");
            append(varDecl.variableName());
            if (varDecl.expression() != null) {
                append(" = ");
                format(varDecl.expression());
            }
        }

        public void format(Statement statement) {
            indent();
            switch (statement) {
                case VariableDeclaration varDecl -> format(varDecl);
                case Return ret -> {
                    append("return ");
                    format(ret.expression());
                }
                case ExecutableCall call -> format((Expression) call);
                case FieldAssignment fieldAssignment -> format((Expression) fieldAssignment);
            }
            append(";\n");
        }

        public <T> void formatList(String prefix, List<T> items, String suffix, Consumer<T> formatter) {
            append(prefix);
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) {
                    append(", ");
                }
                formatter.accept(items.get(i));
            }
            append(suffix);
        }

        public void formatArgumentList(List<Expression> arguments) {
            formatList("(", arguments, ")", this::format);
        }

        public void format(Expression expression) {
            switch (expression) {
                case VariableExpression(String variableName) -> append(variableName);
                case Literal(String literal, Class<?> clazz) -> {
                    if (String.class.equals(clazz)) {
                        append("\"");
                        append(literal);
                        append("\"");
                    } else if (clazz.isEnum()) {
                        format(clazz);
                        append(".");
                        append(literal);
                    } else {
                        append(literal);
                    }
                }
                case MethodCall(ObjectTarget target, String methodName, List<Expression> arguments) -> {
                    if (target != null) {
                        format(target);
                        append(".");
                    }
                    append(methodName);
                    formatArgumentList(arguments);
                }
                case GetFxmlObjectCall(List<Expression> arguments) -> {
                    append("getFxmlObject");
                    formatArgumentList(arguments);
                }
                case SetFxmlObjectCall(List<Expression> arguments) -> {
                    append("setFxmlObject");
                    formatArgumentList(arguments);
                }
                case ConstructorCall(QName className, List<Expression> arguments) -> {
                    append("new ");
                    format(className);
                    formatArgumentList(arguments);
                }
                case LambdaExpression(List<String> paramList, Expression expr) -> {
                    switch (paramList.size()) {
                        case 0 -> append("()");
                        case 1 -> append(paramList.getFirst());
                        default -> append(paramList.stream().collect(Collectors.joining(", ", "(", ")")));
                    }
                    append(" -> ");
                    format(expr);
                }
                case FieldAssignment(ObjectTarget target, String fieldName, Expression valueExpression) -> {
                    if (target != null) {
                        format(target);
                        append(".");
                    }
                    append(fieldName);
                    append(" = ");
                    format(valueExpression);
                }
            }
        }

        public void format(ObjectTarget methodTarget) {
            switch (methodTarget) {
                case ClassTarget(QName className) -> {
                    format(className);
                }
                case ExpressionTarget(Expression expr) -> {
                    format(expr);
                }
            }
        }

        public void format(Imports imports) {
            imports.imports.values().forEach(className -> {
                indent();
                append("import ");
                append(className.toString());
                append(";\n");
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
