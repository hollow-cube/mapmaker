package net.hollowcube.luau.docs.resolve;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.hollowcube.luau.gen.docs.EngineApi;
import net.hollowcube.luau.gen.docs.RawLibrary;

import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/// Reads per-library raw JSONs, builds the global symbol table, runs the cross-module resolver,
/// and either throws [ResolveException] with diagnostics or returns the aggregated [EngineApi].
public final class EngineApiAggregator {

    private static final Gson GSON = new GsonBuilder()
        .serializeNulls()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create();

    private EngineApiAggregator() {
    }

    public static EngineApi aggregate(List<RawLibrary> rawLibraries) {
        var symbols = SymbolTable.build(rawLibraries);
        var diagnostics = new ArrayList<ResolveDiagnostic>();
        for (var lib : rawLibraries) {
            CrossModuleResolver.resolve(lib, symbols, diagnostics);
        }
        if (!diagnostics.isEmpty()) {
            throw new ResolveException(diagnostics);
        }
        var libsByModule = new LinkedHashMap<String, RawLibrary>();
        for (var lib : rawLibraries) {
            libsByModule.put(lib.module(), lib);
        }
        return new EngineApi(EngineApi.CURRENT_SCHEMA_VERSION, EngineApi.KIND, libsByModule);
    }

    public static RawLibrary readRawLibrary(Reader reader) {
        return GSON.fromJson(reader, RawLibrary.class);
    }

    public static EngineApi readEngineApi(Reader reader) {
        return GSON.fromJson(reader, EngineApi.class);
    }

    public static void writeEngineApi(EngineApi api, Writer writer) {
        GSON.toJson(api, writer);
    }

    public static String toJson(EngineApi api) {
        return GSON.toJson(api);
    }
}
