package net.hollowcube.luau.docs.compat;

import net.hollowcube.luau.gen.docs.*;

import java.util.*;

/// Compares an old [EngineApi] snapshot to a new one and produces a [CompatReport].
///
/// V1 uses strict structural type equality with the normalizations in [LuauTypeRelation]. Any
/// type change to a param or return is reported under `BREAKING_PARAM_CHANGED` /
/// `BREAKING_RETURN_CHANGED`. Future v2 will distinguish narrowing from widening.
public final class EngineApiDiff {

    private final List<CompatFinding> findings = new ArrayList<>();

    private EngineApiDiff() {
    }

    public static CompatReport diff(EngineApi oldApi, EngineApi newApi) {
        if (oldApi.schemaVersion() != newApi.schemaVersion()) {
            throw new IllegalStateException("Schema version mismatch: old="
                                            + oldApi.schemaVersion() + ", new=" + newApi.schemaVersion());
        }
        var d = new EngineApiDiff();
        d.compareLibraries(oldApi.libraries(), newApi.libraries());
        return new CompatReport(List.copyOf(d.findings));
    }

    // ===================== Libraries =====================

    private void compareLibraries(Map<String, RawLibrary> oldMap, Map<String, RawLibrary> newMap) {
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

    private void compareLibrary(RawLibrary oldLib, RawLibrary newLib) {
        var name = oldLib.module();
        if (!Objects.equals(oldLib.scope(), newLib.scope())) {
            findings.add(new CompatFinding(DiffCategory.BREAKING_SCOPE_CHANGE, name,
                "library scope changed: " + oldLib.scope() + " → " + newLib.scope()));
        }
        compareMethodList(name, "static", oldLib.staticMethods(), newLib.staticMethods());
        comparePropertyList(name, "static", oldLib.staticProperties(), newLib.staticProperties());
        compareExportList(name, oldLib.exports(), newLib.exports());
    }

    // ===================== Exports =====================

    private void compareExportList(String libName, List<RawExport> oldExports, List<RawExport> newExports) {
        var oldByName = byLuaName(oldExports, RawExport::luaName);
        var newByName = byLuaName(newExports, RawExport::luaName);
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

    private void compareExport(String path, RawExport oldEx, RawExport newEx) {
        if (!Objects.equals(oldEx.superExport(), newEx.superExport())) {
            findings.add(new CompatFinding(DiffCategory.BREAKING_SUPER_CHANGED, path,
                "superExport changed: " + oldEx.superExport() + " → " + newEx.superExport()));
        }
        compareMethodList(path, "method", oldEx.methods(), newEx.methods());
        comparePropertyList(path, "property", oldEx.properties(), newEx.properties());
        compareMetaMethodList(path, oldEx.metaMethods(), newEx.metaMethods());
    }

    // ===================== Methods =====================

    private void compareMethodList(String prefix, String kind, List<RawMethod> oldList, List<RawMethod> newList) {
        var oldByName = byLuaName(oldList, RawMethod::luaName);
        var newByName = byLuaName(newList, RawMethod::luaName);
        for (var key : oldByName.keySet()) {
            var oldM = oldByName.get(key);
            var newM = newByName.get(key);
            var path = prefix + ":" + kind + "." + key;
            if (newM == null) {
                findings.add(new CompatFinding(DiffCategory.BREAKING_REMOVAL, path,
                    kind + " '" + key + "' has been removed"));
                continue;
            }
            compareMethod(path, oldM, newM);
        }
        for (var key : newByName.keySet()) {
            if (!oldByName.containsKey(key)) {
                findings.add(new CompatFinding(DiffCategory.NON_BREAKING_ADDITION,
                    prefix + ":" + kind + "." + key, kind + " '" + key + "' is new"));
            }
        }
    }

    private void compareMethod(String path, RawMethod oldM, RawMethod newM) {
        compareGenerics(path, oldM.generics(), newM.generics());
        compareParams(path, oldM.params(), newM.params());
        compareReturns(path, oldM.returns(), newM.returns());
    }

    private void compareMetaMethodList(String prefix, List<RawMetaMethod> oldList, List<RawMetaMethod> newList) {
        var oldByName = new HashMap<String, RawMetaMethod>();
        var newByName = new HashMap<String, RawMetaMethod>();
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

    private void compareGenerics(String path, List<RawGeneric> oldGen, List<RawGeneric> newGen) {
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

    private void compareParams(String path, List<RawParam> oldParams, List<RawParam> newParams) {
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
            if (!equalsModuloNorm(op.typeExpr(), np.typeExpr())) {
                findings.add(new CompatFinding(DiffCategory.BREAKING_PARAM_CHANGED, p,
                    "param type changed: '" + op.typeExpr() + "' → '" + np.typeExpr() + "'"));
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

    private void compareReturns(String path, List<String> oldRet, List<String> newRet) {
        if (oldRet.size() != newRet.size()) {
            findings.add(new CompatFinding(DiffCategory.BREAKING_RETURN_CHANGED, path,
                "return arity changed: " + oldRet.size() + " → " + newRet.size()));
        }
        int common = Math.min(oldRet.size(), newRet.size());
        for (int i = 0; i < common; i++) {
            if (!equalsModuloNorm(oldRet.get(i), newRet.get(i))) {
                findings.add(new CompatFinding(DiffCategory.BREAKING_RETURN_CHANGED,
                    path + ":return[" + i + "]",
                    "return type changed: '" + oldRet.get(i) + "' → '" + newRet.get(i) + "'"));
            }
        }
    }

    // ===================== Properties =====================

    private void comparePropertyList(String prefix, String kind, List<RawProperty> oldList, List<RawProperty> newList) {
        var oldByName = byLuaName(oldList, RawProperty::luaName);
        var newByName = byLuaName(newList, RawProperty::luaName);
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

    private void compareProperty(String path, RawProperty oldP, RawProperty newP) {
        if (oldP.getter() != null && newP.getter() == null) {
            findings.add(new CompatFinding(DiffCategory.BREAKING_REMOVAL, path + ":getter",
                "getter has been removed"));
        }
        if (oldP.setter() != null && newP.setter() == null) {
            findings.add(new CompatFinding(DiffCategory.BREAKING_REMOVAL, path + ":setter",
                "setter has been removed"));
        }
        if (oldP.getter() != null && newP.getter() != null) {
            compareGetter(path + ":getter", oldP.getter(), newP.getter());
        }
        if (oldP.setter() != null && newP.setter() != null) {
            compareSetter(path + ":setter", oldP.setter(), newP.setter());
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

    private void compareGetter(String path, RawGetter oldG, RawGetter newG) {
        if (!equalsModuloNorm(oldG.returnTypeExpr(), newG.returnTypeExpr())) {
            findings.add(new CompatFinding(DiffCategory.BREAKING_RETURN_CHANGED, path,
                "getter type changed: '" + oldG.returnTypeExpr() + "' → '"
                + newG.returnTypeExpr() + "'"));
        }
    }

    private void compareSetter(String path, RawSetter oldS, RawSetter newS) {
        if (!equalsModuloNorm(oldS.paramTypeExpr(), newS.paramTypeExpr())) {
            findings.add(new CompatFinding(DiffCategory.BREAKING_PARAM_CHANGED, path,
                "setter param type changed: '" + oldS.paramTypeExpr() + "' → '"
                + newS.paramTypeExpr() + "'"));
        }
    }

    // ===================== Helpers =====================

    private static boolean equalsModuloNorm(String oldExpr, String newExpr) {
        if (Objects.equals(oldExpr, newExpr)) return true;
        try {
            return LuauTypeRelation.equalsModuloNorm(oldExpr, newExpr);
        } catch (RuntimeException ignored) {
            // If either side fails to parse, fall back to raw-string equality (already false here).
            return false;
        }
    }

    private static <T> Map<String, T> byLuaName(List<T> items, java.util.function.Function<T, String> keyFn) {
        var out = new java.util.LinkedHashMap<String, T>();
        for (var item : items) out.put(keyFn.apply(item), item);
        return out;
    }
}
