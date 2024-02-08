package no.hal.fxml.translator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.hal.fxml.model.FxmlCode.Import;
import no.hal.fxml.model.QName;

public class ClassResolver {
    
    private final ClassLoader classLoader;

    private final List<Import> imports;

    public ClassResolver(ClassLoader classLoader, List<Import> imports) {
        this.classLoader = classLoader;
        this.imports = new ArrayList<>();
        this.imports.add(new Import(new QName("java.lang", null), true));
        this.imports.addAll(imports);
    }
    
    private Map<QName, Class<?>> classMap = new HashMap<>();

    private Class<?> loadClass(QName typeName) {
        if (classMap.containsKey(typeName)) {
            return classMap.get(typeName);
        }
        Class<?> clazz = null;
        try {
            clazz = classLoader.loadClass(typeName.packageName() + "." + typeName.className());
        } catch (ClassNotFoundException cnfe) {
            // ignore
        }
        classMap.put(typeName, clazz);
        return clazz;
    }

    public Class<?> resolve(QName typeName) {
        if (classMap.containsKey(typeName)) {
            return classMap.get(typeName);
        }
        if (typeName.packageName() != null) {
            return loadClass(typeName);
        }
        String className = typeName.className();
        Class<?> clazz = null;
        // try all non-wildcard imports
        for (var anImport : imports) {
            if ((! anImport.wildcard()) && className.equals(anImport.qName().className())) {
                clazz = loadClass(anImport.qName());
                if (clazz != null) {
                    break;
                }
            }
        }
        if (clazz == null) {
            // try all wildcard imports
            for (var anImport : imports) {
                if (anImport.wildcard() && anImport.qName().className() == null) {
                    clazz = loadClass(new QName(anImport.qName().packageName(), className));
                    if (clazz != null) {
                        break;
                    }
                }
            }
        }
        classMap.put(typeName, clazz);
        return clazz;
    }
}
