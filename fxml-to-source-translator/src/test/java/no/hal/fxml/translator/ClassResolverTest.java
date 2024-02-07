package no.hal.fxml.translator;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import no.hal.fxml.model.FxmlCode.Import;
import no.hal.fxml.model.QName;

public class ClassResolverTest {

    private QName
        java_awt_list_qname = new QName("java.awt", "List"),
        java_util_qname = new QName("java.util", null),
        list_qname = new QName("List"),
        java_util_list_qname = new QName("java.util", "List"),
        collection_qname = new QName("Collection"),
        xyz_qname = new QName("Xyz"),
        java_util_xyz_qname = new QName("java.util", "Xyz")
        ;

    ClassResolver resolver = new ClassResolver(getClass().getClassLoader(), List.of(
        new Import(java_util_qname, true),
        new Import(java_awt_list_qname, false)
    ));    

    @Test
    public void testResolve() throws Exception {
        assertSame(java.awt.List.class, resolver.resolve(list_qname));
        assertSame(java.util.List.class, resolver.resolve(java_util_list_qname));
        assertSame(java.util.Collection.class, resolver.resolve(collection_qname));

        assertSame(java.awt.List.class, resolver.resolve(list_qname));
        assertSame(java.util.List.class, resolver.resolve(java_util_list_qname));
        assertSame(java.util.Collection.class, resolver.resolve(collection_qname));

        assertNull(resolver.resolve(xyz_qname));
        assertNull(resolver.resolve(java_util_xyz_qname));

        assertNull(resolver.resolve(xyz_qname));
        assertNull(resolver.resolve(java_util_xyz_qname));
    }
}