package no.hal.fxml.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import javafx.fxml.FXMLLoader;
import no.hal.fxml.model.FxmlCode.Define;
import no.hal.fxml.model.FxmlCode.Document;
import no.hal.fxml.model.FxmlCode.FxmlElement;
import no.hal.fxml.model.FxmlCode.Import;
import no.hal.fxml.model.FxmlCode.Include;
import no.hal.fxml.model.FxmlCode.InstanceElement;
import no.hal.fxml.model.FxmlCode.InstantiationElement;
import no.hal.fxml.model.FxmlCode.PropertyElement;
import no.hal.fxml.model.FxmlCode.PropertyValue;
import no.hal.fxml.model.FxmlCode.Reference;
import no.hal.fxml.model.FxmlCode.Root;
import no.hal.fxml.model.FxmlCode.StaticProperty;
import no.hal.fxml.model.Instantiation;
import no.hal.fxml.model.Instantiation.Constant;
import no.hal.fxml.model.Instantiation.Constructor;
import no.hal.fxml.model.Instantiation.Factory;
import no.hal.fxml.model.Instantiation.Value;
import no.hal.fxml.model.QName;
import no.hal.fxml.model.ValueExpression;

public class FxmlParser {
    
    public final static String FXML_NAMESPACE_URI = "http://javafx.com/fxml";

    private final XMLEventReader xmlEventReader;

    private FxmlParser(XMLEventReader xmlEventReader) {
        this.xmlEventReader = xmlEventReader;
    }

    public static Document parseFxml(XMLEventReader xmlEventReader) throws Exception  {
        FxmlParser parser = new FxmlParser(xmlEventReader);
        try {
            return parser.parseFxml();
        } finally {
            xmlEventReader.close();
        }
    }

    public static Document parseFxml(Reader reader) throws Exception {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        return FxmlParser.parseFxml(xmlInputFactory.createXMLEventReader(reader));
    }
    public static Document parseFxml(String fxml) throws Exception {
        return parseFxml(new StringReader(fxml));
    }

    public static Document parseFxml(InputStream input) throws Exception {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        return FxmlParser.parseFxml(xmlInputFactory.createXMLEventReader(input));
    }

    public static Document parseFxml(URL url) throws Exception {
        return FxmlParser.parseFxml(url.openStream());
    }

    public static Document parseFxml(File file) throws Exception {
        return FxmlParser.parseFxml(new FileInputStream(file));
    }

    private Document parseFxml() throws XMLStreamException {
        var initialEvent = xmlEventReader.nextEvent();
        if (! initialEvent.isStartDocument()) {
            throw new XMLStreamException("Expected StartDocument");
        }
        XMLEvent nextEvent;
        List<Import> imports = new ArrayList<>();
        while (! (nextEvent = xmlEventReader.nextEvent()).isEndDocument()) {
            if (nextEvent instanceof ProcessingInstruction pi && FXMLLoader.IMPORT_PROCESSING_INSTRUCTION.equals(pi.getTarget())) {
                imports.add(parseImport(pi));
            } else if (nextEvent instanceof StartElement element) {
                FxmlElement fxmlElement = parseStartElement(element);
                if (! (fxmlElement instanceof InstanceElement instanceElement)) {
                    throw new XMLStreamException("Illegal root element: " + fxmlElement);
                }
                return new Document(imports, instanceElement);
            }
        }
        throw new XMLStreamException("Illegal fxml document");
    }

    // element names
    private final static javax.xml.namespace.QName ROOT_QNAME = new javax.xml.namespace.QName(FXML_NAMESPACE_URI, FXMLLoader.ROOT_TAG);
    private final static javax.xml.namespace.QName TYPE_QNAME = new javax.xml.namespace.QName(XMLConstants.NULL_NS_URI, FXMLLoader.ROOT_TYPE_ATTRIBUTE);

    private final static javax.xml.namespace.QName DEFINE_QNAME = new javax.xml.namespace.QName(FXML_NAMESPACE_URI, FXMLLoader.DEFINE_TAG);
    private final static javax.xml.namespace.QName INCLUDE_QNAME = new javax.xml.namespace.QName(FXML_NAMESPACE_URI, FXMLLoader.INCLUDE_TAG);
    private final static javax.xml.namespace.QName INCLUDE_SOURCE_QNAME = new javax.xml.namespace.QName(XMLConstants.NULL_NS_URI, FXMLLoader.INCLUDE_SOURCE_ATTRIBUTE);
    private final static javax.xml.namespace.QName REFERENCE_QNAME = new javax.xml.namespace.QName(FXML_NAMESPACE_URI, FXMLLoader.REFERENCE_TAG);
    private final static javax.xml.namespace.QName REFERENCE_SOURCE_QNAME = new javax.xml.namespace.QName(XMLConstants.NULL_NS_URI, FXMLLoader.REFERENCE_SOURCE_ATTRIBUTE);

    // attribute names
    private final static javax.xml.namespace.QName FACTORY_QNAME = new javax.xml.namespace.QName(FXML_NAMESPACE_URI, FXMLLoader.FX_FACTORY_ATTRIBUTE);
    private final static javax.xml.namespace.QName VALUE_QNAME = new javax.xml.namespace.QName(FXML_NAMESPACE_URI, FXMLLoader.FX_VALUE_ATTRIBUTE);
    private final static javax.xml.namespace.QName CONSTANT_QNAME = new javax.xml.namespace.QName(FXML_NAMESPACE_URI, FXMLLoader.FX_CONSTANT_ATTRIBUTE);

    private final static javax.xml.namespace.QName ID_QNAME = new javax.xml.namespace.QName(FXML_NAMESPACE_URI, FXMLLoader.FX_ID_ATTRIBUTE);
    private final static javax.xml.namespace.QName CONTROLLER_QNAME = new javax.xml.namespace.QName(FXML_NAMESPACE_URI, FXMLLoader.FX_CONTROLLER_ATTRIBUTE);

    private final static Constructor constructorInstantiation = new Constructor();

    private FxmlElement parseStartElement(StartElement element) throws XMLStreamException {
        Attribute idAttr = element.getAttributeByName(ID_QNAME);
        String fxId = (idAttr != null ? idAttr.getValue() : null);
        javax.xml.namespace.QName name = element.getName();
        if (name.getNamespaceURI() == XMLConstants.NULL_NS_URI) {
            if (Character.isUpperCase(name.getLocalPart().charAt(0))) {
                // class name
                Instantiation instantiation = instantiationFor(element, FACTORY_QNAME, Factory::new)
                    .or(() -> instantiationFor(element, VALUE_QNAME, Value::new))
                    .or(() -> instantiationFor(element, CONSTANT_QNAME, Constant::new))
                    .orElse(constructorInstantiation);
                return new InstantiationElement(QName.valueOf(name.getLocalPart()), instantiation, fxId, concat(
                    parseAttributes(element.getAttributes()),
                    parseChildElements(FxmlElement.class)
                ));
            } else {
                // property name
                String propertyName = element.getName().getLocalPart();
                String text = parseText();
                if (text != null) {
                    ValueExpression valueExpression = parseExpression(text);
                    int pos = propertyName.indexOf('.');
                    return (pos < 0
                        ? new PropertyValue(propertyName, valueExpression)
                        : new StaticProperty(propertyName.substring(0, pos), propertyName.substring(pos + 1), valueExpression)
                    );
                }
                return new PropertyElement(element.getName().getLocalPart(),
                    parseChildElements(InstanceElement.class)
                );
            }
        } else if (ROOT_QNAME.equals(name)) {
            return new Root(QName.valueOf(getAttributeValue(element, TYPE_QNAME)), concat(
                parseAttributes(element.getAttributes()),
                parseChildElements(FxmlElement.class)
            ));
        } else if (DEFINE_QNAME.equals(name)) {
            return new Define(parseChildElements(InstantiationElement.class));
        } else if (INCLUDE_QNAME.equals(name)) {
            parseChildElements(null);
            return new Include(getAttributeValue(element, INCLUDE_SOURCE_QNAME));
        } else if (REFERENCE_QNAME.equals(name)) {
            parseChildElements(null);
            return new Reference(getAttributeValue(element, REFERENCE_SOURCE_QNAME));
        }
        throw new XMLStreamException("Illegal/unknown element: " + element);
    }

    private Optional<Instantiation> instantiationFor(StartElement element, javax.xml.namespace.QName name, Function<String, Instantiation> creator) {
        Attribute attr = element.getAttributeByName(name);
        if (attr != null) {
            return Optional.of(creator.apply(attr.getValue()));
        }
        return Optional.empty();
    }

    private List<FxmlElement> concat(List<? extends FxmlElement> list1, List<? extends FxmlElement> list2) {
        List<FxmlElement> all = new ArrayList<>(list1);
        all.addAll(list2);
        return all;
    }

    private String getAttributeValue(StartElement element, javax.xml.namespace.QName attributeName) throws XMLStreamException {
        Attribute typeAttr = element.getAttributeByName(attributeName);
        if (typeAttr == null) {
            throw new XMLStreamException("Missing " + attributeName + " attribute in " + element + " element");
        }
        return typeAttr.getValue();
    }

    private List<PropertyValue> parseAttributes(Iterator<Attribute> attributes) throws XMLStreamException {
        List<PropertyValue> simpleProperties = new ArrayList<>();
        while (attributes.hasNext()) {
            Attribute attr = attributes.next();
            if (attr.getName().getNamespaceURI() == XMLConstants.NULL_NS_URI) {
                simpleProperties.add(new PropertyValue(attr.getName().getLocalPart(), parseExpression(attr.getValue())));
            }
        }
        return simpleProperties;
    }

    private ValueExpression parseExpression(String value) throws XMLStreamException {
        if (value.startsWith(FXMLLoader.BINDING_EXPRESSION_PREFIX)) {
            if (value.endsWith(FXMLLoader.BINDING_EXPRESSION_SUFFIX)) {
                return new ValueExpression.Binding(value.substring(2, value.length() - 1));
            }
        } else if (value.startsWith(FXMLLoader.EXPRESSION_PREFIX)) {
            return new ValueExpression.Reference(value.substring(1));
        } else if (value.startsWith("@")) {
            return new ValueExpression.Location(value.substring(1));
        } else {
            return new ValueExpression.String(value);
        }
        throw new XMLStreamException("Malformed expression");
    }

    private String parseText() throws XMLStreamException {
        XMLEvent nextEvent = xmlEventReader.peek();
        if (nextEvent instanceof Characters chars && (! chars.isWhiteSpace())) {
            String text = chars.getData();
            // skip Characters
            nextEvent = xmlEventReader.nextEvent();
            return text;
        } else {
            return null;
        }
    }

    private <C> List<C> parseChildElements(Class<C> allowsChild) throws XMLStreamException {
        List<C> children = new ArrayList<>();
        XMLEvent nextEvent;
        String textContent = null;
        while (! (nextEvent = xmlEventReader.nextEvent()).isEndElement()) {
            switch (nextEvent) {
                case Characters chars when (! chars.isWhiteSpace()) -> {
                    if (textContent != null || (! children.isEmpty())) {
                        throw new XMLStreamException("Unexpected Characters: " + chars.getData());
                    }
                    textContent = chars.getData();
                }
                case StartElement childElement -> {
                    if (textContent != null) {
                        throw new XMLStreamException("Unexpected StartElement: " + childElement);
                    }
                    FxmlElement child = parseStartElement(childElement);
                    if (allowsChild == null || (! allowsChild.isInstance(child))) {
                        throw new XMLStreamException("Unexpected element: " + child);
                    }
                    children.add((C) child);
                }
                default -> {}
            }
        }
        return children;
    }

    private Import parseImport(ProcessingInstruction pi) throws XMLStreamException {
        var data = pi.getData().trim();
        int pos = data.lastIndexOf('.');
        if (pos < 0) {
            throw new XMLStreamException("Malformed import: " + data);
        }
        String initialPart = data.substring(0, pos);
        String lastSegment = data.substring(pos + 1);
        if ("*".equals(lastSegment)) {
            return new Import(QName.valueOf(initialPart), true);
        } else {
            return new Import(QName.valueOf(data), false);
        }
    }
}
