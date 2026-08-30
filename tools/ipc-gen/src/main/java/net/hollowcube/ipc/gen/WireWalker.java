package net.hollowcube.ipc.gen;

import com.palantir.javapoet.ClassName;
import net.hollowcube.ipc.gen.wire.WireDescriptor.Use;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.*;

/// Walks every type reachable from a wire root and decides whether it is allowed there.
///
/// A wire position may hold a jdk scalar, a `List`/`Set`/`Map<String, _>` of wire types, a
/// `@RuntimeGson` record, an enum that declares `UNKNOWN`, or a sealed interface of such records
/// that permits its generated `Unknown` variant. Anything else is refused with the path it was
/// reached by, because the offending type is usually three records away from the method that
/// dragged it onto the wire. Nothing from the sql-gen package is allowed: the schema and the wire
/// are versioned apart, and sharing an enum between them would let a column change a client.
///
/// Along the way it records how each record is used — sent by a client, returned to one, published
/// as a message, stored as a notification body — which is what decides whether a field added later
/// is safe: a server may add a field to what it returns, but an old client will never send one.
final class WireWalker {

    record Field(RecordComponentElement site, String name, TypeMirror type, boolean nullable) {
    }

    /// @param uses grows as the record is reached from more roots
    record WireRecord(TypeElement element, List<Field> fields, EnumSet<Use> uses) {
    }

    /// @param constants every constant but `UNKNOWN`, in declaration order: the wire values
    record WireEnum(TypeElement element, List<String> constants) {
    }

    /// @param variants wire name to record, in `permits` order, without the unknown variant
    record WireSealed(TypeElement element, ClassName unknown, LinkedHashMap<String, TypeElement> variants) {
    }

    private static final Set<String> SCALARS = Set.of(
        "java.lang.Boolean", "java.lang.Byte", "java.lang.Short", "java.lang.Integer", "java.lang.Long",
        "java.lang.Character", "java.lang.Float", "java.lang.Double", "java.lang.String",
        "java.util.UUID", "java.math.BigDecimal", "java.math.BigInteger",
        // Written as an ISO-8601 string by the adapter Wire registers, so that a time on the wire
        // is a time on both sides rather than a number whose unit each end has to agree on.
        "java.time.Instant");
    private static final Set<String> COLLECTIONS = Set.of("java.util.List", "java.util.Set", "java.util.Collection");
    private static final String MAP = "java.util.Map";

    private final Messager messager;
    private final TreeMap<String, WireRecord> records = new TreeMap<>();
    private final TreeMap<String, WireEnum> enums = new TreeMap<>();
    private final TreeMap<String, WireSealed> sealeds = new TreeMap<>();
    /// Record name and use pairs already walked, which is what stops a recursive record.
    private final Set<String> walked = new HashSet<>();
    private boolean ok = true;

    WireWalker(Messager messager) {
        this.messager = messager;
    }

    boolean ok() {
        return ok;
    }

    Collection<WireRecord> records() {
        return records.values();
    }

    Collection<WireEnum> enums() {
        return enums.values();
    }

    Collection<WireSealed> sealeds() {
        return sealeds.values();
    }

    /// Walks one wire position: a method parameter or return, or the whole record a message or
    /// notification body is.
    ///
    /// @param path how the position is reached, as the diagnostic names it
    void root(Element site, TypeMirror type, Use use, String path) {
        visit(site, type, use, path, Set.of());
    }

    /// Walks a record that is a wire root in its own right, which has to be a `@RuntimeGson` record
    /// like any other; answers false having reported why if it is not.
    boolean rootRecord(TypeElement element, Use use, String path) {
        if (element.getKind() != ElementKind.RECORD) {
            error(element, element.getQualifiedName() + " must be a record", path);
            return false;
        }
        return record(element, element, use, path);
    }

    private void visit(Element site, TypeMirror type, Use use, String path, Set<String> typeVariables) {
        switch (type.getKind()) {
            case BOOLEAN, BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE -> {
            }
            case ARRAY -> visit(site, ((ArrayType) type).getComponentType(), use, path, typeVariables);
            case TYPEVAR -> {
                if (!typeVariables.contains(type.toString())) {
                    error(site, "cannot serialize '" + type + "'; a wire position needs a concrete type", path);
                }
            }
            case DECLARED -> declared(site, (DeclaredType) type, use, path, typeVariables);
            default -> error(site, "cannot serialize '" + type + "'; a wire position needs a concrete type, "
                + "not a wildcard", path);
        }
    }

    private void declared(Element site, DeclaredType type, Use use, String path, Set<String> typeVariables) {
        var element = (TypeElement) type.asElement();
        var name = element.getQualifiedName().toString();
        var arguments = type.getTypeArguments();

        if (SCALARS.contains(name)) return;
        if (COLLECTIONS.contains(name)) {
            if (arguments.size() != 1) {
                error(site, "raw " + WireTypeName.name(element) + " cannot be read back; say what it holds", path);
                return;
            }
            visit(site, arguments.getFirst(), use, path, typeVariables);
            return;
        }
        if (name.equals(MAP)) {
            if (arguments.size() != 2 || !arguments.getFirst().toString().equals("java.lang.String")) {
                error(site, "a wire Map is keyed by String; json objects have no other kind of key", path);
                return;
            }
            visit(site, arguments.get(1), use, path, typeVariables);
            return;
        }
        if (name.startsWith(IpcNames.DB_PACKAGE + ".")) {
            error(site, name + " is a sql-gen type, and the schema and the wire are versioned apart; "
                + "map it to a @RuntimeGson record in modules/ipc", path);
            return;
        }
        if (name.equals("java.lang.Object") || name.startsWith("com.google.gson.")) {
            error(site, name + " is raw json, which nothing can check for compatibility; "
                + "give it a @RuntimeGson record", path);
            return;
        }

        switch (element.getKind()) {
            case ENUM -> enumType(site, element, path);
            case RECORD -> {
                for (var argument : arguments) visit(site, argument, use, path, typeVariables);
                record(site, element, use, path);
            }
            case INTERFACE -> {
                if (element.getModifiers().contains(Modifier.SEALED)) sealed(site, element, use, path);
                else error(site, name + " is not a wire type; an interface on the wire must be sealed so "
                    + "that every variant is known", path);
            }
            default -> error(site, name + " is not a wire type; a wire position holds jdk scalars and "
                + "collections, @RuntimeGson records, enums that declare UNKNOWN, and sealed interfaces "
                + "of those", path);
        }
    }

    private boolean record(Element site, TypeElement element, Use use, String path) {
        var name = element.getQualifiedName().toString();
        var record = records.get(name);
        if (record == null) {
            if (!hasAnnotation(element, IpcNames.RUNTIME_GSON_ANNOTATION)) {
                error(site, name + " is on the wire, so it must be @RuntimeGson", path);
                return false;
            }
            var fields = new ArrayList<Field>();
            for (var component : element.getRecordComponents()) {
                if (hasAnnotation(component, IpcNames.SERIALIZED_NAME_ANNOTATION)
                    || hasAnnotation(component.getAccessor(), IpcNames.SERIALIZED_NAME_ANNOTATION)) {
                    error(component, "component names are the wire names; @SerializedName is not honoured", path);
                }
                fields.add(new Field(component, component.getSimpleName().toString(), component.asType(),
                    Nullability.isNullable(component, component.asType())));
            }
            record = new WireRecord(element, List.copyOf(fields), EnumSet.noneOf(Use.class));
            records.put(name, record);
        }
        record.uses().add(use);

        // Walked once per use rather than once, because a use reaches everything beneath it: a
        // record first seen in a response and later in a request is sent by clients too.
        if (!walked.add(name + "#" + use)) return true;
        var typeVariables = new HashSet<String>();
        for (var parameter : element.getTypeParameters()) typeVariables.add(parameter.getSimpleName().toString());
        for (var field : record.fields()) {
            visit(field.site(), field.type(), use, path + " -> " + element.getSimpleName() + "." + field.name(), typeVariables);
        }
        return true;
    }

    private void enumType(Element site, TypeElement element, String path) {
        var name = element.getQualifiedName().toString();
        if (enums.containsKey(name)) return;

        var constants = new ArrayList<String>();
        var unknown = false;
        for (var field : ElementFilter.fieldsIn(element.getEnclosedElements())) {
            if (field.getKind() != ElementKind.ENUM_CONSTANT) continue;
            var constant = field.getSimpleName().toString();
            if (constant.equals(IpcNames.UNKNOWN_CONSTANT)) {
                unknown = true;
                continue;
            }
            if (hasAnnotation(field, IpcNames.SERIALIZED_NAME_ANNOTATION)) {
                error(field, "constant names are the wire names; @SerializedName is not honoured", path);
            }
            constants.add(constant);
        }
        if (!unknown) {
            error(site, name + " is on the wire, so it must declare an UNKNOWN constant: a build that predates "
                + "a new constant decodes it as UNKNOWN, and an exhaustive switch is what makes that handled", path);
        }
        requirePublic(site, element, path);
        enums.put(name, new WireEnum(element, List.copyOf(constants)));
    }

    private void sealed(Element site, TypeElement element, Use use, String path) {
        var name = element.getQualifiedName().toString();
        var known = sealeds.get(name);
        if (known != null) {
            // Already validated; walk the variants again only to propagate the use.
            for (var variant : known.variants().values()) record(site, variant, use, path);
            return;
        }

        requirePublic(site, element, path);
        for (var method : ElementFilter.methodsIn(element.getEnclosedElements())) {
            if (method.getModifiers().contains(Modifier.ABSTRACT)) {
                error(method, name + " cannot declare abstract methods; its " + IpcNames.UNKNOWN_VARIANT
                    + " variant would have to implement them", path);
            }
        }

        var variants = new LinkedHashMap<String, TypeElement>();
        TypeElement unknown = null;
        for (var permitted : element.getPermittedSubclasses()) {
            var variant = (TypeElement) ((DeclaredType) permitted).asElement();
            if (variant.getSimpleName().contentEquals(IpcNames.UNKNOWN_VARIANT)) {
                unknown = variant;
                requireUnknownShape(variant, path);
                requirePublic(site, variant, path);
                continue;
            }
            if (variant.getKind() != ElementKind.RECORD || !variant.getTypeParameters().isEmpty()) {
                error(variant, "every variant of the sealed wire type " + name + " must be a non-generic record", path);
                continue;
            }
            var variantName = IpcNames.variantName(variant.getSimpleName().toString());
            var variantPath = path + " -> " + element.getSimpleName() + "[" + variantName + "]";
            if (variants.put(variantName, variant) != null) {
                error(variant, "two variants of " + name + " are both '" + variantName + "' on the wire", variantPath);
            }
            for (var component : variant.getRecordComponents()) {
                if (component.getSimpleName().contentEquals(IpcNames.DISCRIMINATOR)) {
                    error(component, "'" + IpcNames.DISCRIMINATOR + "' is the field the variant is named in, "
                        + "so a variant cannot have a component of that name", variantPath);
                }
            }
            requirePublic(site, variant, variantPath);
            record(site, variant, use, variantPath);
        }
        if (unknown == null) {
            error(element, name + " is on the wire, so it must permit an " + IpcNames.UNKNOWN_VARIANT
                + " variant: a build that predates a new variant decodes it as " + IpcNames.UNKNOWN_VARIANT
                + ", and an exhaustive switch is what makes that handled. Write "
                + "`record " + IpcNames.UNKNOWN_VARIANT + "(@Nullable String " + IpcNames.DISCRIMINATOR + ") "
                + "implements " + element.getSimpleName() + " {}` and add it to the permits clause", path);
            return;
        }
        sealeds.put(name, new WireSealed(element, ClassName.get(unknown), variants));
    }

    /// The unknown variant is the one place a variant may be named in the discriminator field, and
    /// it must be exactly that: the adapter builds it from a name it did not recognise and nothing
    /// else, so anything more would have nowhere to come from.
    private void requireUnknownShape(TypeElement variant, String path) {
        if (variant.getKind() != ElementKind.RECORD || !variant.getTypeParameters().isEmpty()) {
            error(variant, IpcNames.UNKNOWN_VARIANT + " must be a non-generic record", path);
            return;
        }
        var components = variant.getRecordComponents();
        // Not toString(): a @Nullable component's type prints with the annotation on it.
        if (components.size() != 1
            || !components.getFirst().getSimpleName().contentEquals(IpcNames.DISCRIMINATOR)
            || !isString(components.getFirst().asType())) {
            error(variant, IpcNames.UNKNOWN_VARIANT + " must be `record " + IpcNames.UNKNOWN_VARIANT
                + "(@Nullable String " + IpcNames.DISCRIMINATOR + ")`: the discriminator it did not "
                + "recognise is all there is to carry", path);
        }
    }

    private static boolean isString(TypeMirror type) {
        return type instanceof DeclaredType declared
            && ((TypeElement) declared.asElement()).getQualifiedName().contentEquals(String.class.getName());
    }

    /// The generated adapters live in `net.hollowcube.ipc`, so what they name has to be visible from there.
    private void requirePublic(Element site, TypeElement element, String path) {
        for (Element enclosing = element; enclosing instanceof TypeElement type; enclosing = type.getEnclosingElement()) {
            var inInterface = type.getEnclosingElement().getKind() == ElementKind.INTERFACE;
            if (!type.getModifiers().contains(Modifier.PUBLIC) && !inInterface) {
                error(site, type.getQualifiedName() + " must be public; the generated adapters name it", path);
                return;
            }
        }
    }

    private static boolean hasAnnotation(Element element, String qualifiedName) {
        for (var mirror : element.getAnnotationMirrors()) {
            var type = (TypeElement) mirror.getAnnotationType().asElement();
            if (type.getQualifiedName().contentEquals(qualifiedName)) return true;
        }
        return false;
    }

    private void error(@Nullable Element site, String message, String path) {
        ok = false;
        messager.printError("ipc: " + message + " (reached via " + path + ")", site);
    }
}
