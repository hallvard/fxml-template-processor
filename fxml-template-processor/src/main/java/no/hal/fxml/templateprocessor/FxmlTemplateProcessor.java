package no.hal.fxml.templateprocessor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class FxmlTemplateProcessor implements StringTemplate.Processor<Node, RuntimeException> {

    private static final String FXML_PREFIX = """
    <?xml version="1.0" encoding="UTF-8"?>
    
    <?import javafx.stage.*?>
    <?import javafx.scene.*?>
    <?import javafx.scene.control.*?>
    <?import javafx.scene.layout.*?>

    <Pane xmlns:fx="http://javafx.com/fxml">
    """;

    private static final String FXML_SUFFIX = """

    </Pane>
    """;

    private final FXMLLoader fxmlLoader;

    private FxmlTemplateProcessor(FXMLLoader fxmlLoader) {
        this.fxmlLoader = fxmlLoader;
    }

    @Override
    public Node process(StringTemplate stringTemplate) throws RuntimeException {
        try {
            var fxml = fxml(stringTemplate.fragments(), stringTemplate.values());
            //System.out.println(stringTemplate.fragments());
            //System.out.println(stringTemplate.values());
            //System.out.println(fxml);
            //System.out.println(fxmlLoader.getNamespace());
            Pane wrapper = fxmlLoader.load(new ByteArrayInputStream(fxml.getBytes()));
            return wrapper.getChildrenUnmodifiable().getFirst();
        } catch (IOException ioex) {
            throw new RuntimeException(ioex);
        }
    }

    private enum ValueType { ELEMENT, ATTRIBUTE, ATTRIBUTE_VALUE }

    private ValueType valueType(List<String> fragments, int pos) {
        int quoteCount = 0;
        for (int i = pos; i >= 0; i--) {
            var fragment = fragments.get(pos);
            for (int j = fragment.length() - 1; j >= 0; j--) {
                char c = fragment.charAt(j);
                if (c == '\"') {
                    quoteCount++;
                } else if (c == '>') {
                    return ValueType.ELEMENT;
                } else if (c == '<') {
                    return quoteCount % 2 == 1 ? ValueType.ATTRIBUTE_VALUE : ValueType.ATTRIBUTE;
                }
            }
        }
        return ValueType.ELEMENT;
    }

    private String fxml(List<String> fragments, List<Object> values) {
        StringBuilder builder = new StringBuilder(FXML_PREFIX);
        for (int i = 0; i < values.size(); i++) {
            builder.append(fragments.get(i));
            var value = values.get(i);
            switch (valueType(fragments, i)) {
                case ATTRIBUTE_VALUE -> {
                    appendVariable(builder, "$", "value_" + i, "", value, fxmlLoader.getNamespace());
                }
                case ATTRIBUTE -> {
                    if (value instanceof Map<?, ?> map) {
                        for (var entry : map.entrySet()) {
                            var attributeName = entry.getKey().toString();
                            builder.append(" ");
                            builder.append(attributeName);
                            appendVariable(builder, "=\"$", "value_" + i + "_" + attributeName, "\"", entry.getValue(), fxmlLoader.getNamespace());
                        }
                    } else {
                        throw new IllegalArgumentException("Only Map values are supported in ATTRIBUTE position");
                    }
                }
                case ELEMENT -> {
                    if (value instanceof Collection col) {
                        int num = 0;
                        for (var val : col) {
                            appendVariable(builder, "<fx:reference source=\"", "value_" + i + "_" + num, "\"/>", val, fxmlLoader.getNamespace());
                        }
                    } else {
                        appendVariable(builder, "<fx:reference source=\"", "value_" + i, "\"/>", value, fxmlLoader.getNamespace());
                    }
                }
            }
        }
        builder.append(fragments.getLast());
        builder.append(FXML_SUFFIX);
        return builder.toString();
    }

    private void appendVariable(StringBuilder builder, String prefix, String varName, String suffix, Object value, Map<String, Object> namespace) {
        builder.append(prefix);
        builder.append(varName);
        builder.append(suffix);
        namespace.put(varName, value);
    }

    //

    public static FxmlTemplateProcessor FXML(FXMLLoader fxmlLoader) {
        return new FxmlTemplateProcessor(fxmlLoader);
    }

    public static FxmlTemplateProcessor FXML() {
        return FXML(new FXMLLoader());
    }
}
