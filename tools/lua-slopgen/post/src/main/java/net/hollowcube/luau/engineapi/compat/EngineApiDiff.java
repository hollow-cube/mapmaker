package net.hollowcube.luau.engineapi.compat;

import net.hollowcube.luau.slopgen.Model;
import net.hollowcube.luau.slopgen.Schema;

import java.util.*;
import java.util.function.Function;

/// Compares an old [Schema] snapshot to a new one and produces a [CompatReport].
///
/// V1 uses strict structural type equality with the normalizations in [LuauTypeRelation]. Any
/// type change to a param or return is reported under `BREAKING_PARAM_CHANGED` /
/// `BREAKING_RETURN_CHANGED`. Future v2 will distinguish narrowing from widening.
public final class EngineApiDiff {

    private final List<CompatFinding> findings = new ArrayList<>();

    private EngineApiDiff() {
    }

    public static CompatReport diff(Schema oldSchema, Schema newSchema) {
        if (oldSchema.schemaVersion() != newSchema.schemaVersion()) {
            throw new IllegalStateException("Schema version mismatch: old="
                                            + oldSchema.schemaVersion() + ", new=" + newSchema.schemaVersion());
        }
        var d = new EngineApiDiff();
        d.compareLibraries(oldSchema.libraries(), newSchema.libraries());
        return new CompatReport(List.copyOf(d.findings));
    }

    // ===================== Libraries =====================

    private void compareLibraries(Map<String, Model.Library> oldMap, Map<String, Model.Library> newMap) {
        for (var name : oldMap.keySet()) {
            var oldLib = oldMap.get(name);
            var newLib = newMap.get(name);
            if (newLib == null) {
                findings.add(new CompatFinding(DiffCategory.BREAKING_REMOVAL, name,
                    "library '" + name + "' has been removed"));
                continue;
            }
            compareLibrary(oldLib, newLib);
        }
        for (var name : newMap.keySet()) {
            if (!oldMap.containsKey(name)) {
                findings.add(new CompatFinding(DiffCategory.NON_BREAKING_ADDITION, name,
                    "library '" + name + "' is new"));
            }
        }
    }

    private void compareLibrary(Model.Library oldLib, Model.Library newLib) {
        var name = oldLib.moduleName();
        if (!Objects.equals(oldLib.scope(), newLib.scope())) {
            findings.add(new CompatFinding(DiffCategory.BREAKING_SCOPE_CHANGE, name,
                "library scope changed: " + oldLib.scope() + " → " + newLib.scope()));
        }
        compareMethodList(name, "static", oldLib.staticMethods(), newLib.staticMethods());
        comparePropertyList(name, "static", oldLib.staticProperties(), newLib.staticProperties());
        compareExportList(name, oldLib.exports(), newLib.exports());
    }

    // ===================== Exports =====================

    private void compareExportList(String libName, List<Model.Export> oldExports, List<Model.Export> newExports) {
        var oldByName = byKey(oldExports, Model.Export::luaName);
        var newByName = byKey(newExports, Model.Export::luaName);
        for (var key : oldByName.keySet()) {
            var oldEx = oldByName.get(key);
            var newEx = newByName.get(key);
            var path = libName + ":" + oldEx.luaName();
            if (newEx == null) {
                findings.add(new CompatFinding(DiffCategory.BREAKING_REMOVAL, path,
                    "export '" + oldEx.luaName() + "' has been removed"));
                continue;
            }
            compareExport(path, oldEx, newEx);
        }
        for (var key : newByName.keySet()) {
            if (!oldByName.containsKey(key)) {
                findings.add(new CompatFinding(DiffCategory.NON_BREAKING_ADDITION,
                    libName + ":" + key, "export '" + key + "' is new"));
            }
        }
    }

    private void compareExport(String path, Model.Export oldEx, Model.Export newEx) {
        var oldSuper = oldEx.superExport() == null ? null : oldEx.superExport().toString();
        var newSuper = newEx.superExport() == null ? null : newEx.superExport().toString();
        if (!Objects.equals(oldSuper, newSuper)) {
            findings.add(new CompatFinding(DiffCategory.BREAKING_SUPER_CHANGED, path,
                "superExport changed: " + oldSuper + " → " + newSuper));
        }
        compareMethodList(path, "method", oldEx.methods(), newEx.methods());
        comparePropertyList(path, "property", oldEx.properties(), newEx.properties());
        compareMetaMethodList(path, oldEx.metaMethods(), newEx.metaMethods());
    }

    // ===================== Methods =====================

    private void compareMethodList(String prefix, String kind, List<Model.Method> oldList, List<Model.Method> newList) {
        var oldByName = byKey(oldList, Model.Method::luaName);
        var newByName = byKey(newList, Model.Method::luaName);
        for (var key : oldByName.keySet()) {
            var oldM = oldByName.get(key);
            var newM = newByName.get(key);
            var path = prefix + ":" + kind + "." + key;
            if (newM == null) {
                findings.add(new CompatFinding(DiffCategory.BREAKING_REMOVAL, path,
                    kind + " '" + key + "' has been removed"));
                continue;
            }
            compareGenerics(path, oldM.generics(), newM.generics());
            compareParams(path, oldM.params(), newM.params());
            compareReturns(path, oldM.returns(), newM.returns());
        }
        for (var key : newByName.keySet()) {
            if (!oldByName.containsKey(key)) {
                findings.add(new CompatFinding(DiffCategory.NON_BREAKING_ADDITION,
                    prefix + ":" + kind + "." + key, kind + " '" + key + "' is new"));
            }
        }
    }

    private void compareMetaMethodList(String prefix, List<Model.MetaMethod> oldList, List<Model.MetaMethod> newList) {
        var oldByName = new HashMap<String, Model.MetaMethod>();
        var newByName = new HashMap<String, Model.MetaMethod>();
        for (var m : oldList) oldByName.put(m.meta(), m);
        for (var m : newList) newByName.put(m.meta(), m);
        for (var key : oldByName.keySet()) {
            var oldM = oldByName.get(key);
            var newM = newByName.get(key);
            var path = prefix + ":meta." + key;
            if (newM == null) {
                findings.add(new CompatFinding(DiffCategory.BREAKING_REMOVAL, path,
                    "metamethod '" + key + "' has been removed"));
                continue;
            }
            compareGenerics(path, oldM.generics(), newM.generics());
            compareParams(path, oldM.params(), newM.params());
            compareReturns(path, oldM.returns(), newM.returns());
        }
        for (var key : newByName.keySet()) {
            if (!oldByName.containsKey(key)) {
                findings.add(new CompatFinding(DiffCategory.NON_BREAKING_ADDITION,
                    prefix + ":meta." + key, "metamethod '" + key + "' is new"));
            }
        }
    }

    private void compareGenerics(String path, List<Model.GenericParam> oldGen, List<Model.GenericParam> newGen) {
        var oldNames = new HashSet<String>();
        var newNames = new HashSet<String>();
        for (var g : oldGen) oldNames.add(g.name() + (g.pack() ? "..." : ""));
        for (var g : newGen) newNames.add(g.name() + (g.pack() ? "..." : ""));
        for (var n : oldNames)
            if (!newNames.contains(n)) {
                findings.add(new CompatFinding(DiffCategory.BREAKING_GENERIC_REMOVED, path,
                    "generic parameter '" + n + "' has been removed"));
            }
        for (var n : newNames)
            if (!oldNames.contains(n)) {
                findings.add(new CompatFinding(DiffCategory.NON_BREAKING_ADDITION, path,
                    "generic parameter '" + n + "' is new"));
            }
    }

    private void compareParams(String path, List<Model.Param> oldParams, List<Model.Param> newParams) {
        int common = Math.min(oldParams.size(), newParams.size());
        for (int i = 0; i < common; i++) {
            var op = oldParams.get(i);
            var np = newParams.get(i);
            var p = path + ":param[" + i + "]";
            if (!Objects.equals(op.name(), np.name())) {
                findings.add(new CompatFinding(DiffCategory.BREAKING_PARAM_CHANGED, p,
                    "param name changed: '" + op.name() + "' → '" + np.name() + "'"));
            }
            if (op.optional() && !np.optional()) {
                findings.add(new CompatFinding(DiffCategory.BREAKING_PARAM_REQUIRED, p,
                    "param '" + np.name() + "' became required"));
            }
            if (!LuauTypeRelation.equalsModuloNorm(op.type(), np.type())) {
                findings.add(new CompatFinding(DiffCategory.BREAKING_PARAM_CHANGED, p,
                    "param type changed: '" + op.type() + "' → '" + np.type() + "'"));
            }
        }
        for (int i = common; i < oldParams.size(); i++) {
            findings.add(new CompatFinding(DiffCategory.BREAKING_REMOVAL,
                path + ":param[" + i + "]",
                "param '" + oldParams.get(i).name() + "' has been removed"));
        }
        for (int i = common; i < newParams.size(); i++) {
            var np = newParams.get(i);
            if (np.optional()) {
                findings.add(new CompatFinding(DiffCategory.NON_BREAKING_ADDITION,
                    path + ":param[" + i + "]",
                    "optional param '" + np.name() + "' is new"));
            } else {
                findings.add(new CompatFinding(DiffCategory.BREAKING_PARAM_ADDED_REQUIRED,
                    path + ":param[" + i + "]",
                    "required param '" + np.name() + "' is new"));
            }
        }
    }

    private void compareReturns(String path, List<Model.Return> oldRet, List<Model.Return> newRet) {
        if (oldRet.size() != newRet.size()) {
            findings.add(new CompatFinding(DiffCategory.BREAKING_RETURN_CHANGED, path,
                "return arity changed: " + oldRet.size() + " → " + newRet.size()));
        }
        int common = Math.min(oldRet.size(), newRet.size());
        for (int i = 0; i < common; i++) {
            if (!LuauTypeRelation.equalsModuloNorm(oldRet.get(i).type(), newRet.get(i).type())) {
                findings.add(new CompatFinding(DiffCategory.BREAKING_RETURN_CHANGED,
                    path + ":return[" + i + "]",
                    "return type changed: '" + oldRet.get(i).type() + "' → '" + newRet.get(i).type() + "'"));
            }
        }
    }

    // ===================== Properties =====================

    private void comparePropertyList(String prefix, String kind, List<Model.Property> oldList, List<Model.Property> newList) {
        var oldByName = byKey(oldList, Model.Property::luaName);
        var newByName = byKey(newList, Model.Property::luaName);
        for (var key : oldByName.keySet()) {
            var oldP = oldByName.get(key);
            var newP = newByName.get(key);
            var path = prefix + ":" + kind + "." + key;
            if (newP == null) {
                findings.add(new CompatFinding(DiffCategory.BREAKING_REMOVAL, path,
                    kind + " '" + key + "' has been removed"));
                continue;
            }
            compareProperty(path, oldP, newP);
        }
        for (var key : newByName.keySet()) {
            if (!oldByName.containsKey(key)) {
                findings.add(new CompatFinding(DiffCategory.NON_BREAKING_ADDITION,
                    prefix + ":" + kind + "." + key, kind + " '" + key + "' is new"));
            }
        }
    }

    private void compareProperty(String path, Model.Property oldP, Model.Property newP) {
        if (oldP.getter() != null && newP.getter() == null) {
            findings.add(new CompatFinding(DiffCategory.BREAKING_REMOVAL, path + ":getter",
                "getter has been removed"));
        }
        if (oldP.setter() != null && newP.setter() == null) {
            findings.add(new CompatFinding(DiffCategory.BREAKING_REMOVAL, path + ":setter",
                "setter has been removed"));
        }
        if (oldP.getter() != null && newP.getter() != null) {
            if (!LuauTypeRelation.equalsModuloNorm(oldP.getter().type(), newP.getter().type())) {
                findings.add(new CompatFinding(DiffCategory.BREAKING_RETURN_CHANGED, path + ":getter",
                    "getter type changed: '" + oldP.getter().type() + "' → '"
                    + newP.getter().type() + "'"));
            }
        }
        if (oldP.setter() != null && newP.setter() != null) {
            if (!LuauTypeRelation.equalsModuloNorm(oldP.setter().type(), newP.setter().type())) {
                findings.add(new CompatFinding(DiffCategory.BREAKING_PARAM_CHANGED, path + ":setter",
                    "setter param type changed: '" + oldP.setter().type() + "' → '"
                    + newP.setter().type() + "'"));
            }
        }
        if (oldP.getter() == null && newP.getter() != null) {
            findings.add(new CompatFinding(DiffCategory.NON_BREAKING_ADDITION,
                path + ":getter", "getter is new"));
        }
        if (oldP.setter() == null && newP.setter() != null) {
            findings.add(new CompatFinding(DiffCategory.NON_BREAKING_ADDITION,
                path + ":setter", "setter is new"));
        }
    }

    // ===================== Helpers =====================

    private static <T> Map<String, T> byKey(List<T> items, Function<T, String> keyFn) {
        var out = new LinkedHashMap<String, T>();
        for (var item : items) out.put(keyFn.apply(item), item);
        return out;
    }
}
