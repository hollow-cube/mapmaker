package net.hollowcube.luau.ap.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.List;

public final class ProcUtil {

    public static void logStackTrace(@NotNull Messager log, @NotNull Element elem, @NotNull Throwable t) {
        log.printMessage(javax.tools.Diagnostic.Kind.ERROR, t.getMessage(), elem);
        for (var trace : t.getStackTrace()) {
            log.printMessage(javax.tools.Diagnostic.Kind.ERROR, trace.toString(), elem);
        }
        if (t.getCause() != null) {
            logStackTrace(log, elem, t.getCause());
        }
    }

    public static @NotNull List<? extends Element> getAnnotatedMembers(@NotNull Elements elements, @NotNull TypeElement elem, @NotNull TypeName annotation) {
        return elements.getAllMembers(elem).stream()
                .filter(e -> e.getEnclosingElement() == elem) // Normally this includes entries from superclass, filter them out.
                .filter(e -> e.getAnnotationMirrors().stream()
                        .anyMatch(a -> annotation.equals(TypeName.get(a.getAnnotationType()))))
                .toList();
    }

    public static @Nullable AnnotationMirror getAnnotation(@NotNull Element elem, @NotNull ClassName annotation) {
        return elem.getAnnotationMirrors().stream()
                .filter(a -> annotation.compareTo((ClassName) TypeName.get(a.getAnnotationType())) == 0)
                .findFirst().orElse(null);
    }

    public static <T> @NotNull T getAnnotationValue(@NotNull AnnotationMirror annotation, @NotNull String name, Class<T> valueType) {
        return annotation.getElementValues().entrySet().stream()
                .filter(e -> e.getKey().getSimpleName().contentEquals(name))
                .map(entry -> {
                    Object value = entry.getValue().getValue();
                    if (!valueType.isAssignableFrom(value.getClass()))
                        throw new IllegalArgumentException("Expected " + valueType + " but got " + value.getClass());
                    return valueType.cast(value);
                })
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Annotation does not have a value named " + name));
    }

}
