package no.hal.fxml.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TypeRefTest {
    
    @Test
    public void testValueOf() {
        assertEquals(
            new TypeRef("java.util.List"),
            TypeRef.valueOf("java.util.List")
        );
        assertEquals(
            new TypeRef("java.util.List", new TypeRef("String")),
            TypeRef.valueOf("java.util.List<String>")
        );
        assertEquals(
            new TypeRef("java.util.Map", new TypeRef("String"), new TypeRef("java.util.Collection", new TypeRef("String"))),
            TypeRef.valueOf("java.util.Map<String, java.util.Collection<String>>")
        );
        assertEquals(
            new TypeRef("java.util.Map", new TypeRef("java.util.Collection", new TypeRef("String")), new TypeRef("String")),
            TypeRef.valueOf("java.util.Map<java.util.Collection<String>, String>")
        );
    }
}
