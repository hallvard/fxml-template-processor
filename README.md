# fxml-to-source-translator

This repo contains an FXML to Java source translator ((`fxml-to-source-translator`)),
so you can pre-compile FXML and load it at runtime without using reflection.
The ultimate goal is to use fxml in quarkus with minimal reflection,
by integrating with quarkus and its [`quarkus-fx`](https://docs.quarkiverse.io/quarkus-fx/dev/index.html) extension.

A supporting maven plugin (`fxml-maven-plugin`) runs the translator during a build.
A sample project (`fxml-to-source-translator-sample`) shows how to use both.

## build and run

```
mvn clean install -f fxml-runtime
mvn clean install -f fxml-to-source-translator
mvn clean install -f fxml-maven-plugin
mvn clean install quarkus:build quarkus:run -f fxml-to-source-translator-sample
```

# fxml-template-processor (obsolete, since string templates were removed in Java 22)

String templates (preview in Java 21) allow you to inject expression values into strings,
so you don't need to use explicit string concatenation, StringBuilder or formatting.

An important point of template strings is building other data types from the string and
constrain and validate the input accordingly.

The `fxml-template-processor` project contains a String template processor for FXML,
so you can use template strings for FXML code and convert to `Node` structures with `FXMLLoader`.

## Current features

Three injection cases are supported:

### attribute values

```
    var labelText = "Hi 1";
    var label1 = FXML()."""
    <Label fx:id="label1" text="\{labelText}"/>
    """;
```

Here the value of the `labelText` variable is provided as the `text` attribute of a `Label`.

The type of the value must match the type of the corresponding property.

### attributes

```
    var labelAttribute = Map.of("text", "Hi 2");
    var label2 = FXML()."""
    <Label fx:id="label2" \{labelAttribute}/>
    """;
```

The key/value pairs of the `labelAttribute` `Map` are injected into a Label as attributes,
in this case a `text` attribute.

The types of the values must match the types of the corresponding properties.

### elements

```
    var label3 = new Label("Hi 3");
    var label3Pane = FXML()."""
    <Pane>
        \{label3}
    </Pane>
    """;
```

The value of `label3` is injected as a child of a `Pane`.

The type of the value must match the type of the corresponding property. This is often the implicit `children` property
of Pane, but can be any property depending on the surrounding markup.

## Usage

Import with

```
import static no.hal.fxml.templateprocessor.FxmlTemplateProcessor.FXML;
```

Then use `FXML()`. in front of the template string to process the result with a fresh `FXMLLoader`.
You can provide a pre-rigged `FXMLLoader` as argument, e.g. with the controller and/or location set, but
note that it will the modified as a side-effect and hence, should not be reused.

As a convenience, there is an `FXML()` method overload taking an object,
which is set as the controller of a fresh `FXMLLoader` instance.
