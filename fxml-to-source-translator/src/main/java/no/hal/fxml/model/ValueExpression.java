package no.hal.fxml.model;

// creates an instance given an InstantiationElement
public sealed interface ValueExpression {
    
    // "Hello!"
    public record String(java.lang.String value) implements ValueExpression {
    }

    // $label
    public record IdReference(java.lang.String source) implements ValueExpression {
    }

    // ${label.text}
    public record Binding(java.lang.String source) implements ValueExpression {
    }

    // @myUrl.png
    public record Location(java.lang.String location) implements ValueExpression {
    }

    // #method
    public record MethodReference(java.lang.String methodName) implements ValueExpression {
    }    
}
