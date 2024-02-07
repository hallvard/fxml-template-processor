package no.hal.fxml.model;

// creates an instance given an InstantiationElement
public sealed interface Instantiation {
    
    public record Constructor() implements Instantiation {
    }

    // fx:factory="observableArrayList"
    public record Factory(String methodName) implements Instantiation {
    }

    // fx:value="1.0"
    public record Value(String valueString) implements Instantiation {
    }

    // fx:constant="NEGATIVE_INFINITY"
    public record Constant(String constantName) implements Instantiation {
    }
}
