package net.hollowcube.ipc.gen;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

/// Whether a wire position may hold null: a primitive never can, and anything else can when it is
/// marked `@Nullable` — by simple name, so jetbrains and jspecify both count.
///
/// Every package in `ipc` is `@NotNullByDefault`, so an unmarked reference type is non-null, which
/// is the assumption the generated server enforces with a 400 and the descriptor records.
final class Nullability {

    static boolean isNullable(Element element, TypeMirror type) {
        if (type.getKind().isPrimitive()) return false;
        if (hasNullable(element.getAnnotationMirrors()) || hasNullable(type.getAnnotationMirrors())) return true;
        // A record component's annotation may only have landed on the accessor, depending on the
        // annotation's targets.
        return element instanceof RecordComponentElement component
            && hasNullable(component.getAccessor().getAnnotationMirrors());
    }

    private static boolean hasNullable(List<? extends AnnotationMirror> mirrors) {
        for (var mirror : mirrors) {
            if (mirror.getAnnotationType().asElement().getSimpleName().contentEquals("Nullable")) return true;
        }
        return false;
    }

    private Nullability() {
    }
}
