package no.hal.fxml.model;

import java.util.ArrayList;
import java.util.List;

public record TypeRef(QName typeName, List<TypeRef> typeParams) {

    public TypeRef(QName typeName, TypeRef... typeParams) {
        this(typeName, List.of(typeParams));
    }

    public TypeRef(String typeName, TypeRef... typeParams) {
        this(QName.valueOf(typeName), List.of(typeParams));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        toString(builder);
        return builder.toString();
        
    }
    private void toString(StringBuilder builder) {
        builder.append(typeName);
        if (typeParams != null && (! typeParams.isEmpty())) {
            builder.append("<");
            int pos = builder.length();
            for (var typeParam : typeParams()) {
                if (pos < builder.length()) {
                    builder.append(", ");
                }
                typeParam.toString(builder);
            }
            builder.append(">");
        }
    }

    public static TypeRef valueOf(String typeRef) {
        return new TypeRefParser(typeRef).parse();
    }
}

class TypeRefParser {

    private final String typeRef;
    private int position = 0;

    public TypeRefParser(String typeRef) {
        this.typeRef = typeRef;
    }

    private boolean isAt(int pos, char c) {
        return (position < typeRef.length() && typeRef.charAt(pos) == c);
    }
    private boolean isAt(int pos, String cs) {
        return (position < typeRef.length() && cs.indexOf(typeRef.charAt(pos)) >= 0);
    }

    private String parseTypeName() {
        int start = position;
        while (position < typeRef.length() && (! isAt(position, "<,>"))) {
            position++;
        }    
        return typeRef.substring(start, position).trim();
    }    

    private List<TypeRef> parseTypeRefs() {
        List<TypeRef> typeRefs = new ArrayList<>();
        while (! isAt(position, '>')) {
            typeRefs.add(parse());
            if (isAt(position, ',')) {
                position++;
            } else if (! isAt(position, ">,")) {
                throw new IllegalArgumentException("Malformed TypeRef, expected , or > @ " + position + ": " + typeRef);
            }
        }
        return typeRefs;
    }

    TypeRef parse() {
        String typeName = parseTypeName();
        List<TypeRef> params = List.of();
        if (isAt(position, '<')) {
            position++;
            params = parseTypeRefs();
            if (! isAt(position, '>')) {
                throw new IllegalArgumentException("Malformed TypeRef, expected > @ " + position + ": " + typeRef);
            }
            position++;
            while (position < typeRef.length() && typeRef.charAt(position) == ' ') {
                position++;
            }
        }
        return new TypeRef(QName.valueOf(typeName), params);
    }

    public static void main(String[] args) {
        System.out.println(TypeRef.valueOf("java.util.Map<java.util.Collection<String>, String>"));
    }
}