package no.hal.fxml.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaCode {

    public record Imports(Map<String, QName> imports) {

        public Imports {
            imports = new HashMap<>(imports);
        }

        private boolean autoImports(QName qName) {
            return qName.packageName() == null || "java.lang".equals(qName.packageName());
        }

        public boolean imports(QName qName) {
            return autoImports(qName) || qName.equals(imports.get(qName.className()));
        }

        public boolean importIfAvailable(QName qName) {
            if (autoImports(qName)) {
                return false;
            }
            String key = qName.className();
            if (imports.containsKey(key)) {
                return qName.equals(imports.get(key));
            } else {
                imports.put(key, qName);
                return true;
            }
        }
    }

    public record ClassDeclaration(QName className, TypeRef superClass, List<TypeRef> superInterfaces, List<Member> members) {
        public ClassDeclaration(QName className, TypeRef superClass, List<TypeRef> superInterfaces, Member... members) {
            this(className, superClass, superInterfaces, List.of(members));
        }
    }

    public sealed interface Member permits ConstructorDeclaration, MethodDeclaration, VariableDeclaration {
        public String getName();
    }

    public record ConstructorDeclaration(String modifiers, String className, List<VariableDeclaration> parameters, List<Statement> body)
        implements Member {
        public ConstructorDeclaration(String modifiers, String className, List<VariableDeclaration> parameters) {
            this(modifiers, className, parameters, null);
        }
        @Override
        public String getName() {
            return className;
        }
    }

    public record MethodDeclaration(String modifiers, String methodName, TypeRef returnType, List<VariableDeclaration> parameters, List<Statement> body)
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
        permits Comment, VariableDeclaration, Return, ExecutableCall, FieldAssignment {
    }

    public record Comment(String... commentLines) implements Statement {
        public static Comment line(String commentLine) {
            return new Comment(commentLine);
        }
        public static Comment block(String commentLines) {
            return new Comment(("\n" + commentLines + "\n").split("\n"));
        }
        @Override
        public String toString() {
            if (commentLines.length == 1) {
                return "// " + commentLines[0];
            } else {
                return Stream.of(commentLines).collect(Collectors.joining("*", "/*\n", "\n*/"));
            }
        }
    }

    public record VariableDeclaration(String modifiers, TypeRef typeName, String variableName, Expression expression)
        implements Statement, Member {
        public VariableDeclaration(TypeRef typeName, String variableName, Expression expression) {
            this(null, typeName, variableName, expression);
        }
        public VariableDeclaration(QName typeName, String variableName, Expression expression) {
            this(new TypeRef(typeName), variableName, expression);
        }
        public VariableDeclaration(String typeRef, String variableName, Expression expression) {
            this(TypeRef.valueOf(typeRef), variableName, expression);
        }
        public static VariableDeclaration instantiation(String className, String variableName) {
            return new VariableDeclaration(className, variableName, new ConstructorCall(className));
        }
        public static VariableDeclaration parameter(TypeRef typeName, String variableName) {
            return new VariableDeclaration(typeName, variableName, null);
        }
        @Override
        public String toString() {
            String s = "%s %s".formatted(typeName != null ? typeName : "var", variableName, expression);
            if (modifiers != null) {
                s = modifiers + " " + s;
            }
            if (expression != null) {
                s = s + " = " + expression;               
            }
            return s;
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
        permits VariableExpression, Literal, Cast, ExecutableCall, LambdaExpression, LambdaMethodReference, FieldAssignment {
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

    public record Cast(TypeRef type, Expression expression)
        implements Expression {
        @Override
        public String toString() {
            return "((%s) %s)".formatted(type, expression);
        }
    }

    public sealed interface ObjectTarget
        permits ClassTarget, ExpressionTarget {

        public static ObjectTarget thisTarget() {
            return new ExpressionTarget("this");
        }
    }

    public record ClassTarget(QName className) implements ObjectTarget {
        public ClassTarget(String className) {
            this(QName.valueOf(className));
        }
        @Override
        public String toString() {
            return "%s".formatted(className);
        }
    }

    public record ExpressionTarget(Expression expression) implements ObjectTarget {
        public ExpressionTarget(String variableName) {
            this(new VariableExpression(variableName));
        }
        @Override
        public String toString() {
            return expression.toString();
        }
    }

    public sealed interface ExecutableCall extends Expression, Statement
        permits MethodCall, ConstructorCall {

        public List<Expression> arguments();
    }

    public static String statements2String(List<Statement> statements) {
        return statements.stream().map(statement -> "%s;".formatted(statement)).collect(Collectors.joining("\n"));
    }
    
    public static String expression2String(Expression expression) {
        return expression.toString();
    } 
    public static <T> String list2String(List<T> items) {
        return items != null ? items.stream().map(Object::toString).collect(Collectors.joining(", ")) : "";
    }

    public record MethodCall(ObjectTarget target, String methodName, List<Expression> arguments)
        implements ExecutableCall {
        public MethodCall(ObjectTarget target, String methodName, Expression singleArg) {
            this(target, methodName, singleArg != null ? Collections.singletonList(singleArg) : Collections.emptyList());
        }
        public MethodCall(Expression expression, String methodName, Expression singleArgOrNull) {
            this(new ExpressionTarget(expression), methodName, singleArgOrNull);
        }
        public MethodCall(String variableName, String methodName, Expression singleArgOrNull) {
            this(new ExpressionTarget(variableName), methodName, singleArgOrNull);
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
            String targetPrefix = (target != null ? target + "." : "");
            return "%s%s(%s)".formatted(targetPrefix, methodName, JavaCode.list2String(arguments));
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

    public record LambdaMethodReference(ObjectTarget target, String methodName)
        implements Expression {
        @Override
        public String toString() {
            return "%s::%s".formatted(target, methodName);
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
        public void spaces(int i) {
            while (i-- > 0) {
                append(" ");
            }
        }
        public void space() {
            spaces(1);
        }

        public <T> void withIndentation(int d, T t, BiConsumer<Formatter, T> formatter) {
            indentLevel += d;
            formatter.accept(this, t);
            indentLevel -= d;
        }
        public <T> void withIndentation(T t, BiConsumer<Formatter, T> formatter) {
            withIndentation(1, t, formatter);
        }

        //

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

        //

        public void format(TypeRef classRef) {
            format(classRef.typeName());
            if (classRef.typeParams() != null && (! classRef.typeParams().isEmpty())) {
                formatList("<", classRef.typeParams(), ">", Formatter::format);
            }
        }

        public void format(ClassDeclaration classDeclaration) {
            append("public class ");
            append(classDeclaration.className.className());
            if (classDeclaration.superClass() != null) {
                append(" extends ");
                format(classDeclaration.superClass());
            }
            if (classDeclaration.superInterfaces() != null && (! classDeclaration.superInterfaces().isEmpty())) {
                append(" implements ");
                formatList("", classDeclaration.superInterfaces(), "", Formatter::format);
            }
            append(" {\n");
            withIndentation(classDeclaration.members(), (f, m) -> m.forEach(f::format));
            append("}\n");
        }

        public void format(Member member) {
            newline();
            switch (member) {
                case ConstructorDeclaration consDecl -> format(consDecl);
                case MethodDeclaration methodDecl -> format(methodDecl);
                case VariableDeclaration varDecl -> formatStatement(varDecl);
            }
        }

        public void format(ConstructorDeclaration cons) {
            indent();
            if (append(cons.modifiers())) {
                space();
            }
            append(cons.className());
            formatList("(", cons.parameters(), ") {\n", Formatter::format);
            withIndentation(cons, (f, c) -> {
                if (c.body() != null) {
                    f.format(c.body());
                } else {
                    indent();
                    formatList("super(", c.parameters().stream().map(VariableDeclaration::variableName).toList(), ");\n", Formatter::append);
                }
            });
            indent();
            append("}\n");
        }

        public void format(MethodDeclaration method) {
            indent();
            if (append(method.modifiers())) {
                space();
            }
            if (method.returnType() != null) {
                format(method.returnType());
            } else {
                append("void");
            }
            space();
            append(method.methodName());
            formatList("(", method.parameters(), ") {\n", Formatter::format);
            withIndentation(method.body(), Formatter::format);
            indent();
            append("}\n");
        }

        public void format(List<Statement> statements) {
            statements.forEach(this::formatStatement);
        }

        public void format(VariableDeclaration varDecl) {
            if (varDecl.modifiers() != null) {
                append(varDecl.modifiers());
                space();
            }
            if (varDecl.typeName() != null) {
                format(varDecl.typeName());
            } else {
                append("var");
            }
            space();
            append(varDecl.variableName());
            if (varDecl.expression() != null) {
                append(" = ");
                formatExpression(varDecl.expression());
            }
        }

        public void formatStatement(Statement statement) {
            indent();
            switch (statement) {
                case Comment(String[] commentLines) -> {
                    if (commentLines.length == 1) {
                        append("// ");
                        append(commentLines[0]);
                    } else {
                        append("/*"); append(commentLines[0]); newline();
                        for (int i = 1; i < commentLines.length - 2; i++) {
                            append(" * "); append(commentLines[i]); newline();
                        }
                        append(commentLines[commentLines.length - 1]); append(" */"); newline();
                    }
                }
                case VariableDeclaration varDecl -> format(varDecl);
                case Return ret -> {
                    append("return ");
                    formatExpression(ret.expression());
                }
                case ExecutableCall call -> formatExpression(call);
                case FieldAssignment fieldAssignment -> formatExpression(fieldAssignment);
            }
            if (! (statement instanceof Comment)) {
                append(";");
            }
            newline();
        }

        public <T> void formatList(String prefix, List<T> items, String suffix, BiConsumer<Formatter, T> formatter) {
            append(prefix);
            for (int i = 0; items != null && i < items.size(); i++) {
                if (i > 0) {
                    append(", ");
                }
                formatter.accept(this, items.get(i));
            }
            append(suffix);
        }

        public void formatArgumentList(List<Expression> arguments) {
            formatList("(", arguments, ")", Formatter::formatExpression);
        }

        public void formatExpression(Expression expression) {
            switch (expression) {
                case VariableExpression(String variableName) -> append(variableName);
                case Literal(String literal, Class<?> clazz) -> {
                    if (String.class.equals(clazz)) {
                        append("\"");
                        append(literal);
                        append("\"");
                    } else if (clazz.isEnum()) {
                        format(QName.valueOf(clazz.getName()));
                        append(".");
                        append(literal);
                    } else {
                        append(literal);
                    }
                }
                case Cast(TypeRef type, Expression expr) -> {
                    append("((");
                    format(type);
                    append(") ");
                    formatExpression(expr);
                    append(")");
                }
                case MethodCall(ObjectTarget target, String methodName, List<Expression> arguments) -> {
                    if (target != null) {
                        format(target);
                        append(".");
                    }
                    append(methodName);
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
                    formatExpression(expr);
                }
                case LambdaMethodReference(ObjectTarget target, String methodName) -> {
                    format(target);
                    append("::");
                    append(methodName);
                }
                case FieldAssignment(ObjectTarget target, String fieldName, Expression valueExpression) -> {
                    if (target != null) {
                        format(target);
                        append(".");
                    }
                    append(fieldName);
                    append(" = ");
                    formatExpression(valueExpression);
                }
            }
        }

        public void format(ObjectTarget methodTarget) {
            switch (methodTarget) {
                case ClassTarget(QName className) -> {
                    format(className);
                }
                case ExpressionTarget(Expression expr) -> {
                    formatExpression(expr);
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

    public static String toJavaSource(ClassDeclaration classDeclaration) {
        Imports imports = new Imports(Map.<String, QName>of());
        JavaCode.Formatter formatter = new Formatter(imports);
        formatter.append("""
            package %s;

            """.formatted(classDeclaration.className().packageName())
        );

        String classDecl = formatter.format(classDeclaration, (f, cd) -> {
            f.format(cd);
        });
        
        formatter.format(imports);
        formatter.newline();
        formatter.append(classDecl);

        return formatter.toString();
    }
}
