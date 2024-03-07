package no.hal.fxml.model;

// creates an instance given an InstantiationElement
public sealed interface ValueExpression {

    java.lang.String toShortString();
    
    // "Hello!"
    public record String(java.lang.String value) implements ValueExpression {
        @Override
        public java.lang.String toShortString() {
            return value;
        }
    }

    // $label
    public record IdReference(java.lang.String source) implements ValueExpression {
        @Override
        public java.lang.String toShortString() {
            return "$%s".formatted(source);
        }
    }

    // ${label.text}
    public record Binding(java.lang.String source) implements ValueExpression {
        @Override
        public java.lang.String toShortString() {
            return "\"{%s}\"".formatted(source);
        }
    }

    // @myUrl.png
    public record Location(java.lang.String location) implements ValueExpression {
        @Override
        public java.lang.String toShortString() {
            return "@%s".formatted(location);
        }
    }

    // #method
    public record MethodReference(java.lang.String methodName) implements ValueExpression {
        @Override
        public java.lang.String toShortString() {
            return "#%s".formatted(methodName);
        }
    }    
}
