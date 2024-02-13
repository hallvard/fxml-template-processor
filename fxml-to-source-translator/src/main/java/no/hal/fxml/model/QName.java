package no.hal.fxml.model;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record QName(String packageName, String className) {

    public QName {
        int pos = (className != null ? className.indexOf('$') : -1);
        if (pos >= 0) {
            className = className.substring(0, pos) + "." + className.substring(pos + 1);
        }
    }

    public QName(String className) {
        this(null, className);
    }
    
    public static QName valueOf(String name) {
        int pos = name.lastIndexOf('.');
        if (pos < 0) {
            if (! Character.isUpperCase(name.charAt(0))) {
                throw new IllegalArgumentException("Class name must start with uppercase letter: " + name);
            }
            return new QName(name);
        } else {
            if (Character.isUpperCase(name.charAt(pos + 1))) {
                return new QName(name.substring(0, pos), name.substring(pos + 1));
            } else {
                return new QName(name, null);
            }
        }
    }

    @Override
    public String toString() {
        return toString(packageName, className);
    }

    public static String toString(String... segments) {
        return Stream.of(segments).filter(Objects::nonNull).collect(Collectors.joining("."));
    }
}