package no.hal.fxml.translator;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javafx.scene.control.Label;
import javafx.scene.paint.Color;

public class ReflectionHelperTest {

    ReflectionHelper reflectionHelper = new ReflectionHelper();

    @Test
    public void testGetConstructor() {
        Assertions.assertTrue(reflectionHelper.getConstructor(Color.class, ReflectionHelper.NAMED_ARGS_CONSTRUCTOR_TEST).isPresent());
        Assertions.assertFalse(reflectionHelper.getConstructor(Color.class, ReflectionHelper.NO_ARGS_CONSTRUCTOR_TEST).isPresent());
        Assertions.assertTrue(reflectionHelper.getConstructor(Label.class, ReflectionHelper.NO_ARGS_CONSTRUCTOR_TEST).isPresent());
        Assertions.assertFalse(reflectionHelper.getConstructor(Label.class, ReflectionHelper.NAMED_ARGS_CONSTRUCTOR_TEST).isPresent());
    }

    @Test
    public void testgetNamedConstructorArgs() {
        Constructor<?> cons = reflectionHelper.getNamedArgsConstructor(Color.class).get();
        Assertions.assertEquals(List.of("red", "green", "blue", "opacity"), new ArrayList<>(reflectionHelper.getNamedConstructorArgs(cons).keySet()));
    }

    @Test
    public void testGetGetter() {
        Assertions.assertTrue(reflectionHelper.getGetter(Label.class, "text").isPresent());
        Assertions.assertFalse(reflectionHelper.getGetter(Label.class, "xyz").isPresent());
    }

    @Test
    public void testGetSetter() {
        Assertions.assertTrue(reflectionHelper.getSetter(Label.class, "text").isPresent());
        Assertions.assertTrue(reflectionHelper.getSetter(Label.class, "text", String.class).isPresent());
        Assertions.assertFalse(reflectionHelper.getSetter(Label.class, "xyz").isPresent());
    }

    @Test
    public void testImplements() {
        Assertions.assertTrue(reflectionHelper.implementsList(ArrayList.class));
        Assertions.assertFalse(reflectionHelper.implementsList(Collection.class));
        Assertions.assertTrue(reflectionHelper.implementsMap(HashMap.class));
        Assertions.assertFalse(reflectionHelper.implementsMap(Collection.class));
    }
}