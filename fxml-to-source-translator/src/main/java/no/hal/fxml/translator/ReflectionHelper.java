package no.hal.fxml.translator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javafx.beans.DefaultProperty;
import javafx.beans.NamedArg;

public class ReflectionHelper {

    public String methodName(String prefix, String propertyName) {
        if (Character.isLowerCase(propertyName.charAt(0))) {
            propertyName = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        }
        return prefix + propertyName;
    }
    public String propertyName(String prefix, String methodName) {
        if (methodName.startsWith(prefix)) {
            return (Character.isUpperCase(methodName.charAt(prefix.length()))
                ? Character.toLowerCase(methodName.charAt(prefix.length())) + methodName.substring(prefix.length() + 1)
                : methodName.substring(prefix.length())
            );
        }
        return null;
    }

    private record ClassExecutable(Class<?> clazz, String methodName, Predicate<Executable> test) {}

    private Map<ClassExecutable, Executable> classExecutables = new HashMap<>();

    public static final Predicate<Executable> NO_METHOD_PARAMS_TEST = exec -> ((Method) exec).getParameterCount() == 0;
    public static final Predicate<Executable> SINGLE_METHOD_PARAM_TEST = exec -> ((Method) exec).getParameterCount() == 1;

    private record ParamsKey(Class<?> firstParamType, int paramCount) {}

    private Map<ParamsKey, Predicate<Executable>> singleMethodParamMap = new HashMap<>();

    public Predicate<Executable> firstMethodParamTest(Class<?> exec, int paramCount) {
        var key = new ParamsKey(exec, paramCount);
        return singleMethodParamMap.computeIfAbsent(key, pt -> (Executable ex) -> {
            Method m = (Method) ex;
            return m.getParameterCount() == pt.paramCount && m.getParameterTypes()[0] == pt.firstParamType;
        });
    }
    public Predicate<Executable> singleMethodParamTest(Class<?> paramType) {
        return firstMethodParamTest(paramType, 1);
    }

    public Optional<Method> getMethod(Class<?> clazz, String methodName, Predicate<Executable> test) {
        return getMethod(new ClassExecutable(clazz, methodName, test));
    }

    private Optional<Method> getMethod(ClassExecutable classMethod) {
        if (classExecutables.containsKey(classMethod)) {
            Method m = (Method) classExecutables.get(classMethod);
            return m != null ? Optional.of(m) : Optional.empty();
        }
        for (Method method : classMethod.clazz.getMethods()) {
            if (method.getName().equals(classMethod.methodName) && classMethod.test.test(method)) {
                classExecutables.put(classMethod, method);
                return Optional.of(method);
            }
        }
        classExecutables.put(classMethod, null);
        return Optional.empty();
    }

    public static final Predicate<Executable> NO_ARGS_CONSTRUCTOR_TEST = exec -> ((Constructor<?>) exec).getParameterCount() == 0;

    public Optional<Constructor<?>> getNoArgsConstructor(Class<?> clazz) {
        return getConstructor(clazz, NO_ARGS_CONSTRUCTOR_TEST);
    }

    public static final Predicate<Executable> NAMED_ARGS_CONSTRUCTOR_TEST = exec -> {
        Constructor<?> cons = ((Constructor<?>) exec);
        if (cons.getParameterCount() == 0) {
            return false;
        }
        for (var annotations : cons.getParameterAnnotations()) {
            if (! Stream.of(annotations).anyMatch(annotation -> annotation instanceof NamedArg)) {
                return false;
            }
        }
        return true;
    };

    public Optional<Constructor<?>> getNamedArgsConstructor(Class<?> clazz) {
        return getConstructor(clazz, NAMED_ARGS_CONSTRUCTOR_TEST);
    }

    public record NamedArgInfo(String name, Class<?> type, String defaultValue) {}

    public Map<String, NamedArgInfo> getNamedConstructorArgs(Constructor<?> cons) {
        // order is important
        Map<String, NamedArgInfo> namedArgs = new LinkedHashMap<>();
        Annotation[][] parameterAnnotations = cons.getParameterAnnotations();
        Class<?>[] parameterTypes = cons.getParameterTypes();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (var annotation : parameterAnnotations[i]) {
                if (annotation instanceof NamedArg namedArg) {
                    namedArgs.put(namedArg.value(), new NamedArgInfo(namedArg.value(), parameterTypes[i], namedArg.defaultValue()));
                }
            }
        }
        return namedArgs;
    }

    public Optional<Constructor<?>> getConstructor(Class<?> clazz, Predicate<Executable> test) {
        var key = new ClassExecutable(clazz, null, test);
        if (classExecutables.containsKey(key)) {
            return Optional.of((Constructor<?>) classExecutables.get(key));
        }
        return getConstructor(key);
    }

    private Optional<Constructor<?>> getConstructor(ClassExecutable classConstructor) {
        if (classExecutables.containsKey(classConstructor)) {
            return Optional.of((Constructor<?>) classExecutables.get(classConstructor));
        }
        for (Constructor<?> cons : classConstructor.clazz.getConstructors()) {
            if (classConstructor.test.test(cons)) {
                classExecutables.put(classConstructor, cons);
                return Optional.of(cons);
            }
        }
        classExecutables.put(classConstructor, null);
        return Optional.empty();
    }

    public Optional<Method> getGetter(Class<?> clazz, String propertyName) {
        return getMethod(clazz, methodName("get", propertyName), NO_METHOD_PARAMS_TEST);
    }

    public Optional<Method> getSetter(Class<?> clazz, String propertyName) {
        return getMethod(clazz, methodName("set", propertyName), SINGLE_METHOD_PARAM_TEST);
    }

    public Optional<Method> getSetter(Class<?> clazz, String propertyName, Class<?> paramType) {
        return getMethod(clazz, methodName("set", propertyName), singleMethodParamTest(paramType));
    }

    //

    public boolean isSubTypeOf(Class<?> superType, Class<?> subType) {
        return superType.isAssignableFrom(subType);
    }

    public boolean implementsList(Class<?> subType) {
        return isSubTypeOf(List.class, subType);
    }

    public boolean implementsMap(Class<?> subType) {
        return isSubTypeOf(Map.class, subType);
    }

    //

    public Optional<String> getDefaultProperty(Class<?> clazz) {
        DefaultProperty defaultProperty = clazz.getAnnotation(DefaultProperty.class);
        if (defaultProperty != null) {
            return Optional.of(defaultProperty.value());
        }
        return Optional.empty();
    }

    //

    public <T extends Annotation> Map<Member, T> findAnnotatedMembers(Class<?> clazz, Class<T> annotationClass) {
        Map<Member, T> members = new LinkedHashMap<>();
        for (var field : clazz.getDeclaredFields()) {
            T annotation = field.getAnnotation(annotationClass);
            if (annotation != null) {
                members.put(field, annotation);
            }
        }
        for (var method : clazz.getDeclaredMethods()) {
            T annotation = method.getAnnotation(annotationClass);
            if (annotation != null) {
                members.put(method, annotation);
            }
        }
        return members;
    }

    //

    public Method getMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        outer: for (var method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == paramTypes.length) {
                var methodParamTypes = method.getParameterTypes();
                for (int i = 0; i < paramTypes.length; i++) {
                    if (! paramTypes[i].isAssignableFrom(methodParamTypes[i])) {
                        continue outer;
                    }
                }
                return method;
            }
        }
        return null;
    }
}
