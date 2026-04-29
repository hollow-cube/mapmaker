package net.hollowcube.luau.docs.compat;

import net.hollowcube.luau.gen.docs.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EngineApiDiffTest {

    @Test
    void identityHasNoFindings() {
        var api = singleLibApi("@t/x",
            List.of(method("noop", List.of(), List.of("nil"))),
            List.of(),
            List.of());
        var report = EngineApiDiff.diff(api, api);
        assertEquals(0, report.findings().size());
    }

    @Test
    void removedLibraryFlagged() {
        var oldApi = singleLibApi("@t/r", List.of(), List.of(), List.of());
        var newApi = empty();
        var report = EngineApiDiff.diff(oldApi, newApi);
        assertEquals(1, report.findings().size());
        assertEquals(DiffCategory.BREAKING_REMOVAL, report.findings().get(0).category());
    }

    @Test
    void addedLibraryNonBreaking() {
        var oldApi = empty();
        var newApi = singleLibApi("@t/a", List.of(), List.of(), List.of());
        var report = EngineApiDiff.diff(oldApi, newApi);
        assertEquals(1, report.findings().size());
        assertEquals(DiffCategory.NON_BREAKING_ADDITION, report.findings().get(0).category());
        assertFalse(report.hasBreakingChanges());
    }

    @Test
    void scopeChangeBreaking() {
        var oldApi = api(libBuilder("@t/s").scope("REQUIRE").build());
        var newApi = api(libBuilder("@t/s").scope("GLOBAL").build());
        var report = EngineApiDiff.diff(oldApi, newApi);
        assertTrue(report.findings().stream()
            .anyMatch(f -> f.category() == DiffCategory.BREAKING_SCOPE_CHANGE));
    }

    @Test
    void removedMethodBreaking() {
        var oldApi = singleLibApi("@t/m",
            List.of(method("noop", List.of(), List.of("nil"))),
            List.of(),
            List.of());
        var newApi = singleLibApi("@t/m", List.of(), List.of(), List.of());
        var report = EngineApiDiff.diff(oldApi, newApi);
        assertTrue(report.findings().stream()
            .anyMatch(f -> f.category() == DiffCategory.BREAKING_REMOVAL));
    }

    @Test
    void changedReturnTypeBreaking() {
        var oldApi = singleLibApi("@t/rt",
            List.of(method("get", List.of(), List.of("number"))),
            List.of(), List.of());
        var newApi = singleLibApi("@t/rt",
            List.of(method("get", List.of(), List.of("string"))),
            List.of(), List.of());
        var report = EngineApiDiff.diff(oldApi, newApi);
        assertTrue(report.findings().stream()
            .anyMatch(f -> f.category() == DiffCategory.BREAKING_RETURN_CHANGED));
    }

    @Test
    void changedParamTypeBreaking() {
        var oldApi = singleLibApi("@t/pt",
            List.of(method("set", List.of(new RawParam("x", false, "number")), List.of("nil"))),
            List.of(), List.of());
        var newApi = singleLibApi("@t/pt",
            List.of(method("set", List.of(new RawParam("x", false, "string")), List.of("nil"))),
            List.of(), List.of());
        var report = EngineApiDiff.diff(oldApi, newApi);
        assertTrue(report.findings().stream()
            .anyMatch(f -> f.category() == DiffCategory.BREAKING_PARAM_CHANGED));
    }

    @Test
    void optionalToRequiredBreaking() {
        var oldApi = singleLibApi("@t/or",
            List.of(method("set", List.of(new RawParam("x", true, "number")), List.of("nil"))),
            List.of(), List.of());
        var newApi = singleLibApi("@t/or",
            List.of(method("set", List.of(new RawParam("x", false, "number")), List.of("nil"))),
            List.of(), List.of());
        var report = EngineApiDiff.diff(oldApi, newApi);
        assertTrue(report.findings().stream()
            .anyMatch(f -> f.category() == DiffCategory.BREAKING_PARAM_REQUIRED));
    }

    @Test
    void newRequiredParamBreaking() {
        var oldApi = singleLibApi("@t/np",
            List.of(method("set", List.of(), List.of("nil"))),
            List.of(), List.of());
        var newApi = singleLibApi("@t/np",
            List.of(method("set", List.of(new RawParam("x", false, "number")), List.of("nil"))),
            List.of(), List.of());
        var report = EngineApiDiff.diff(oldApi, newApi);
        assertTrue(report.findings().stream()
            .anyMatch(f -> f.category() == DiffCategory.BREAKING_PARAM_ADDED_REQUIRED));
    }

    @Test
    void newOptionalParamNonBreaking() {
        var oldApi = singleLibApi("@t/no",
            List.of(method("set", List.of(), List.of("nil"))),
            List.of(), List.of());
        var newApi = singleLibApi("@t/no",
            List.of(method("set", List.of(new RawParam("x", true, "number")), List.of("nil"))),
            List.of(), List.of());
        var report = EngineApiDiff.diff(oldApi, newApi);
        assertFalse(report.hasBreakingChanges());
    }

    @Test
    void unionMemberOrderingNormalized() {
        var oldApi = singleLibApi("@t/u",
            List.of(method("get", List.of(), List.of("string | number"))),
            List.of(), List.of());
        var newApi = singleLibApi("@t/u",
            List.of(method("get", List.of(), List.of("number | string"))),
            List.of(), List.of());
        var report = EngineApiDiff.diff(oldApi, newApi);
        assertFalse(report.hasBreakingChanges(),
            "union ordering normalization should make these equivalent");
    }

    @Test
    void optionalAndUnionWithNilNormalized() {
        var oldApi = singleLibApi("@t/o",
            List.of(method("get", List.of(), List.of("Player | nil"))),
            List.of(),
            List.of(new RawExport("Player", "fixtures.x.Player", "", null, true,
                List.of(), List.of(), List.of())));
        var newApi = singleLibApi("@t/o",
            List.of(method("get", List.of(), List.of("Player?"))),
            List.of(),
            List.of(new RawExport("Player", "fixtures.x.Player", "", null, true,
                List.of(), List.of(), List.of())));
        var report = EngineApiDiff.diff(oldApi, newApi);
        assertFalse(report.hasBreakingChanges());
    }

    @Test
    void schemaMismatchThrows() {
        var oldApi = new EngineApi(1, "engine-api", Map.of());
        var newApi = new EngineApi(2, "engine-api", Map.of());
        assertThrows(IllegalStateException.class, () -> EngineApiDiff.diff(oldApi, newApi));
    }

    @Test
    void getterTypeChangeBreaking() {
        var oldApi = singleLibApi("@t/gt",
            List.of(),
            List.of(new RawProperty("count",
                new RawGetter("getCount", "", "number"), null)),
            List.of());
        var newApi = singleLibApi("@t/gt",
            List.of(),
            List.of(new RawProperty("count",
                new RawGetter("getCount", "", "string"), null)),
            List.of());
        var report = EngineApiDiff.diff(oldApi, newApi);
        assertTrue(report.findings().stream()
            .anyMatch(f -> f.category() == DiffCategory.BREAKING_RETURN_CHANGED));
    }

    @Test
    void setterRemovedBreaking() {
        var oldApi = singleLibApi("@t/sr",
            List.of(),
            List.of(new RawProperty("count",
                new RawGetter("getCount", "", "number"),
                new RawSetter("setCount", "", "value", "number"))),
            List.of());
        var newApi = singleLibApi("@t/sr",
            List.of(),
            List.of(new RawProperty("count",
                new RawGetter("getCount", "", "number"), null)),
            List.of());
        var report = EngineApiDiff.diff(oldApi, newApi);
        assertTrue(report.findings().stream()
            .anyMatch(f -> f.category() == DiffCategory.BREAKING_REMOVAL
                           && f.path().contains("setter")));
    }

    @Test
    void superExportChangeBreaking() {
        var oldApi = singleLibApi("@t/se",
            List.of(), List.of(),
            List.of(new RawExport("Pup", "fixtures.x.Pup", "", "fixtures.x.Dog", true,
                List.of(), List.of(), List.of())));
        var newApi = singleLibApi("@t/se",
            List.of(), List.of(),
            List.of(new RawExport("Pup", "fixtures.x.Pup", "", "fixtures.x.Animal", true,
                List.of(), List.of(), List.of())));
        var report = EngineApiDiff.diff(oldApi, newApi);
        assertTrue(report.findings().stream()
            .anyMatch(f -> f.category() == DiffCategory.BREAKING_SUPER_CHANGED));
    }

    @Test
    void removedGenericBreaking() {
        var oldApi = singleLibApi("@t/g",
            List.of(new RawMethod("noop", "noop", "",
                List.of(new net.hollowcube.luau.gen.docs.RawGeneric("T", false)),
                List.of(),
                List.of("nil"))),
            List.of(), List.of());
        var newApi = singleLibApi("@t/g",
            List.of(method("noop", List.of(), List.of("nil"))),
            List.of(), List.of());
        var report = EngineApiDiff.diff(oldApi, newApi);
        assertTrue(report.findings().stream()
            .anyMatch(f -> f.category() == DiffCategory.BREAKING_GENERIC_REMOVED));
    }

    // ----- helpers -----

    private static EngineApi empty() {
        return new EngineApi(1, "engine-api", Map.of());
    }

    private static EngineApi api(RawLibrary... libs) {
        var map = new java.util.LinkedHashMap<String, RawLibrary>();
        for (var l : libs) map.put(l.module(), l);
        return new EngineApi(1, "engine-api", map);
    }

    private static EngineApi singleLibApi(String module, List<RawMethod> staticMethods,
                                          List<RawProperty> staticProperties,
                                          List<RawExport> exports) {
        var lib = libBuilder(module)
            .staticMethods(staticMethods)
            .staticProperties(staticProperties)
            .exports(exports)
            .build();
        return api(lib);
    }

    private static RawMethod method(String name, List<RawParam> params, List<String> returns) {
        return new RawMethod(name, name, "", List.of(), params, returns);
    }

    private static LibBuilder libBuilder(String module) {
        return new LibBuilder(module);
    }

    private static final class LibBuilder {
        private final String module;
        private String scope = "REQUIRE";
        private List<RawMethod> staticMethods = List.of();
        private List<RawProperty> staticProperties = List.of();
        private List<RawExport> exports = List.of();

        LibBuilder(String module) {
            this.module = module;
        }

        LibBuilder scope(String s) {
            this.scope = s;
            return this;
        }

        LibBuilder staticMethods(List<RawMethod> m) {
            this.staticMethods = m;
            return this;
        }

        LibBuilder staticProperties(List<RawProperty> p) {
            this.staticProperties = p;
            return this;
        }

        LibBuilder exports(List<RawExport> e) {
            this.exports = e;
            return this;
        }

        RawLibrary build() {
            return new RawLibrary(1, "raw-library", module, scope,
                "fixtures." + module.replace("@", "").replace("/", "_"),
                "", staticMethods, staticProperties, exports);
        }
    }

    @SuppressWarnings("unused")
    private static RawMetaMethod meta(String metaName) {
        return new RawMetaMethod(metaName, metaName, false, "", List.of(), List.of(), List.of());
    }
}
