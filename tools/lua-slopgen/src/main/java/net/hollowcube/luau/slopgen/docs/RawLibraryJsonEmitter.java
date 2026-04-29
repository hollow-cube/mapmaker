package net.hollowcube.luau.slopgen.docs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.palantir.javapoet.TypeName;
import net.hollowcube.luau.gen.docs.*;
import net.hollowcube.luau.slopgen.model.*;

import java.util.ArrayList;
import java.util.List;

/// Translates a [LibrarySpec] into a [RawLibrary] DTO and serializes it to pretty-printed
/// JSON. Output is consumed by the docs-module aggregator (Luau type parsing, cross-module
/// resolution, compat checking).
public final class RawLibraryJsonEmitter {

    private static final Gson GSON = new GsonBuilder()
        .serializeNulls()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create();

    private RawLibraryJsonEmitter() {
    }

    public static String toJson(LibrarySpec spec) {
        return GSON.toJson(toRaw(spec));
    }

    public static RawLibrary toRaw(LibrarySpec spec) {
        return new RawLibrary(
            RawLibrary.CURRENT_SCHEMA_VERSION,
            RawLibrary.KIND,
            spec.moduleName(),
            spec.scope().name(),
            spec.sourceType().toString(),
            spec.docs().description(),
            mapMethods(spec.staticMethods()),
            mapProperties(spec.staticProperties()),
            mapExports(spec.exports()));
    }

    private static List<RawMethod> mapMethods(List<MethodSpec> methods) {
        var out = new ArrayList<RawMethod>(methods.size());
        for (var m : methods) out.add(mapMethod(m));
        return out;
    }

    private static RawMethod mapMethod(MethodSpec m) {
        var docs = m.docs();
        return new RawMethod(
            m.luaName(),
            m.javaMethodName(),
            docs.description(),
            mapGenerics(docs),
            mapParams(docs),
            new ArrayList<>(docs.returns()));
    }

    private static List<RawProperty> mapProperties(List<PropertySpec> properties) {
        var out = new ArrayList<RawProperty>(properties.size());
        for (var p : properties) {
            RawGetter getter = null;
            RawSetter setter = null;
            if (p.getter() != null) getter = mapGetter(p.getter());
            if (p.setter() != null) setter = mapSetter(p.setter());
            out.add(new RawProperty(p.luaName(), getter, setter));
        }
        return out;
    }

    private static RawGetter mapGetter(AccessorSpec acc) {
        var docs = acc.docs();
        var ret = docs.returns().isEmpty() ? "" : docs.returns().get(0);
        return new RawGetter(acc.javaMethodName(), docs.description(), ret);
    }

    private static RawSetter mapSetter(AccessorSpec acc) {
        var docs = acc.docs();
        String name = "";
        String typeExpr = "";
        if (!docs.params().isEmpty()) {
            var p = docs.params().get(0);
            name = p.name();
            typeExpr = p.typeExpr();
        }
        return new RawSetter(acc.javaMethodName(), docs.description(), name, typeExpr);
    }

    private static List<RawExport> mapExports(List<ExportSpec> exports) {
        var out = new ArrayList<RawExport>(exports.size());
        for (var ex : exports) out.add(mapExport(ex));
        return out;
    }

    private static RawExport mapExport(ExportSpec ex) {
        return new RawExport(
            ex.luaName(),
            ex.javaType().toString(),
            ex.docs().description(),
            ex.superExport() == null ? null : typeNameString(ex.superExport()),
            ex.isFinal(),
            mapMethods(ex.methods()),
            mapProperties(ex.properties()),
            mapMetaMethods(ex.metaMethods()));
    }

    private static List<RawMetaMethod> mapMetaMethods(List<MetaSpec> metaMethods) {
        var out = new ArrayList<RawMetaMethod>(metaMethods.size());
        for (var m : metaMethods) {
            var docs = m.docs();
            out.add(new RawMetaMethod(
                m.meta().name(),
                m.javaMethodName(),
                m.isVoid(),
                docs.description(),
                mapGenerics(docs),
                mapParams(docs),
                new ArrayList<>(docs.returns())));
        }
        return out;
    }

    private static List<RawGeneric> mapGenerics(MemberDocs docs) {
        var out = new ArrayList<RawGeneric>(docs.generics().size());
        for (var g : docs.generics()) out.add(new RawGeneric(g.name(), g.pack()));
        return out;
    }

    private static List<RawParam> mapParams(MemberDocs docs) {
        var out = new ArrayList<RawParam>(docs.params().size());
        for (var p : docs.params()) out.add(new RawParam(p.name(), p.optional(), p.typeExpr()));
        return out;
    }

    private static String typeNameString(TypeName t) {
        return t.toString();
    }
}
