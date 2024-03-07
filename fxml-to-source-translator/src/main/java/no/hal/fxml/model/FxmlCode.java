package no.hal.fxml.model;

import java.util.Collections;
import java.util.List;

public class FxmlCode {

    // all elements
    public interface FxmlElement {
        String toShortString();
    }

    public interface FxmlParent<C extends FxmlElement> {
        public List<C> children();
    }

    // elements that (somehow) provides an instance
    public sealed interface InstanceElement extends FxmlElement
        permits BeanElement, Reference, Include {
    }

    // elements that have properties that can be set
    public sealed interface BeanElement extends InstanceElement, FxmlParent<FxmlElement>
        permits InstantiationElement, Root {
        public QName beanType();
    }

    public record Document(List<Import> imports, InstanceElement instanceElement, QName controllerClassName) {}

    // <?import javafx.scene.*?>
    public record Import(QName qName, boolean wildcard) {
    }

    // <fx:root type="javafx.scene.layout.VBox" xmlns:fx="http://javafx.com/fxml">
    public record Root(QName typeName, List<FxmlElement> children) implements BeanElement {
        @Override
        public String toShortString() {
            return "<fx:root type=\"%s\">".formatted(typeName);
        }
        @Override
        public QName beanType() {
            return typeName;
        }
    }

    // <fx:define>
    public record Define(List<InstantiationElement> children) implements FxmlElement, FxmlParent<InstantiationElement> {
        @Override
        public String toShortString() {
            return "<fx:define>";
        }
    }

    // elements that create a new instance
    public record InstantiationElement(QName className, Instantiation instantiation, String id, List<FxmlElement> children)
        implements BeanElement {
        public InstantiationElement(QName className, Instantiation instantiation, String id) {
            this(className, instantiation, id, Collections.emptyList());
        }
        @Override
        public String toShortString() {
            return id != null ? "<%s fx:id=\"%s\">".formatted(className.className(), id) : "<%s>".formatted(className.className());
        }
        @Override
        public QName beanType() {
            return className;
        }
    }

    public record Reference(String source) implements InstanceElement {
        @Override
        public String toShortString() {
            return "<fx:reference source=\"%s\"/>".formatted(source);
        }
    }

    public record Include(String id, String source) implements InstanceElement {
        @Override
        public String toShortString() {
            return "<fx:include source=\"%s\"/>".formatted(source);
        }
    }

    // bean property, sets a property given a class and property name

    public sealed interface BeanProperty extends FxmlElement
        permits PropertyElement, PropertyValue, StaticProperty {
        public String propertyName();
    }

    // <minHeight><Double .../></minHeight>
    public record PropertyElement(String propertyName, List<InstanceElement> children)
        implements BeanProperty, FxmlParent<InstanceElement> {
        public PropertyElement(String propertyName, InstanceElement instanceElement) {
            this(propertyName, List.of(instanceElement));
        }
        @Override
        public String toShortString() {
            return "<%s>".formatted(propertyName);
        }
    }

    // text="My Label"
    // <text>Hello, World!</text>
    public record PropertyValue(String propertyName, ValueExpression value)
        implements BeanProperty {
            @Override
            public String toShortString() {
                return "%s=\"%s\"".formatted(propertyName, value.toShortString());
            }
        }

    // <GridPane.rowIndex>0</GridPane.rowIndex>
    public record StaticProperty(String className, String propertyName, ValueExpression value)
        implements BeanProperty {
        @Override
        public String toShortString() {
            return "<%s.%s>%s<%s.%s>".formatted(className, propertyName, value.toShortString(), className, propertyName);
        }
    }
}
