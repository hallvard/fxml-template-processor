package no.hal.fxml.parser;

import java.io.StringReader;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import no.hal.fxml.model.FxmlCode.Define;
import no.hal.fxml.model.FxmlCode.Document;
import no.hal.fxml.model.FxmlCode.Import;
import no.hal.fxml.model.FxmlCode.InstantiationElement;
import no.hal.fxml.model.FxmlCode.PropertyElement;
import no.hal.fxml.model.FxmlCode.PropertyValue;
import no.hal.fxml.model.FxmlCode.Reference;
import no.hal.fxml.model.Instantiation.Constructor;
import no.hal.fxml.model.Instantiation.Factory;
import no.hal.fxml.model.Instantiation.Value;
import no.hal.fxml.model.QName;
import no.hal.fxml.model.ValueExpression;

public class FxmlParserTest {

    private void testFxmlParser(String fxmlSource, Document expected) throws Exception {
        Document actual = FxmlParser.parseFxml(new StringReader(fxmlSource));
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testFxmlParser() throws Exception {
        testFxmlParser("""
            <?xml version="1.0" encoding="UTF-8"?>
            <?import javafx.scene.control.*?>
            <?import javafx.scene.layout.Pane?>
            <?import javafx.collections.*?>
        
            <Pane xmlns:fx="http://javafx.com/fxml">
                <fx:define>
                    <String fx:id="item2" fx:value="Item 2"/>
                </fx:define>
                <Label fx:id="label1" text="Hi!"/>
                <ListView>
                    <items>
                        <FXCollections fx:factory="observableArrayList">
                            <String fx:value="Item 1"/>
                            <fx:reference source="item2"/>
                        </FXCollections>
                    </items>
                </ListView>
            </Pane>
            """,
            new Document(
                List.of(
                    new Import(new QName("javafx.scene.control", null), true),
                    new Import(new QName("javafx.scene.layout", "Pane"), false),
                    new Import(new QName("javafx.collections", null), true)
                ),
                new InstantiationElement(new QName("Pane"), new Constructor(), null,
                    List.of(
                        new Define(List.of(
                            new InstantiationElement(new QName("String"), new Value("Item 2"), "item2")
                        )),
                        new InstantiationElement(new QName("Label"), new Constructor(), "label1",
                            List.of(
                                new PropertyValue("text", new ValueExpression.String("Hi!"))
                            )
                        ),
                        new InstantiationElement(new QName("ListView"), new Constructor(), null,
                            List.of(
                                new PropertyElement("items",
                                    List.of(
                                        new InstantiationElement(new QName("FXCollections"), new Factory("observableArrayList"), null,
                                            List.of(
                                                new InstantiationElement(new QName("String"), new Value("Item 1"), null),
                                                new Reference("item2")
                                            )
                                        )            
                                    )
                                )
                            )
                        )
                    )
                )
            )
        );
    }
}

/*
Document[
    imports=[
        Import[qName=QName[packageName=javafx.scene.control, className=null], wildcard=true],
        Import[qName=QName[packageName=javafx.scene.layout, className=Pane], wildcard=false]
    ],
    instanceElement=InstantiationElement[className=QName[packageName=null, className=Pane], instantiation=Constructor[], id=null,
        children=[
            InstantiationElement[className=QName[packageName=null, className=Label], instantiation=Constructor[], id=label1,
                children=[SimpleProperty[propertyName=text, value=StringExpression[value=Hi!]]]],
            InstantiationElement[className=QName[packageName=null, className=ListView], instantiation=Constructor[], id=null,
                children=[PropertyElement[propertyName=items,
                    children=[InstantiationElement[className=QName[packageName=null, className=FXCollections],
                        instantiation=Factory[methodName=observableArrayList], id=null,
                        children=[
                            InstantiationElement[className=QName[packageName=null, className=String],
                                instantiation=Factory[methodName=Item 1], id=null, children=[]],
                            InstantiationElement[className=QName[packageName=null, className=String],
                                instantiation=Factory[methodName=Item 2], id=null, children=[]]]
                            ]
                        ]
                    ]
                ]
            ]
        ]
    ]
]
 */
