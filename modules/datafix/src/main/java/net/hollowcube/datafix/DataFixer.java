package net.hollowcube.datafix;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.hollowcube.datafix.util.TranscoderValue;
import net.hollowcube.datafix.util.Value;
import net.hollowcube.datafix.versions.v0xxx.*;
import net.hollowcube.datafix.versions.v1xxx.*;
import net.hollowcube.datafix.versions.v2xxx.*;
import net.hollowcube.datafix.versions.v3xxx.*;
import net.hollowcube.datafix.versions.v4xxx.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Transcoder;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public class DataFixer {
    private static final AtomicInteger state = new AtomicInteger(); // 0=not built, 1=building, 2=built

    // Building state
    private static final List<DataType> dataTypes = new ArrayList<>();
    private static final Int2ObjectMap<DataTypeBuilder> builders = new Int2ObjectOpenHashMap<>();

    // Built state
    private static int minVersion = 99, maxVersion = MinecraftServer.DATA_VERSION;
    public static OptimizedSchema[] schemas; // DataTypeV2 -> Schema

    // Builder Methods

    static void addDataType(@NotNull DataType knownType) {
        dataTypes.add(knownType);
    }

    public static void addFixVersions(@NotNull List<Supplier<DataVersion>> versions) {
        if (state.get() != 0) throw new IllegalArgumentException("DataFixer is already built.");
        for (var version : versions) {
            var built = version.get();
            maxVersion = Math.max(maxVersion, built.version());
        }
    }

    public static void buildModel() {
        int currentState = state.get();
        if (currentState != 0) return;

        for (var externalFix : ServiceLoader.load(ExternalDataFix.class)) {
            if (!(externalFix instanceof DataVersion dataVersion))
                throw new UnsupportedOperationException("ExternalDataFix must implement DataVersion");
            maxVersion = Math.max(maxVersion, dataVersion.version());
        }

        int maxId = dataTypes.stream().mapToInt(DataType::id).max().orElse(0);
        schemas = new OptimizedSchema[maxId + 1];

        // Create the initial schema for each data type and its children
        for (int i = 0; i <= maxId; i++) {
            var builder = builders.get(i);
            if (builder == null) continue;

            var children = new HashMap<String, OptimizedSchema>();
            builder.idMap.forEach((id, child) -> {
                var fixes = new ArrayList<>(builder.fixes);
                fixes.addAll(child.fixes);
                var fixSpans = createFixSpans(fixes);

                var properties = new ArrayList<>(builder.properties);
                properties.addAll(child.properties);

                children.put(id, new OptimizedSchema(
                        child.id, Map.of(), mapFixVersions(fixSpans.first()),
                        fixSpans.first(), fixSpans.second(),
                        List.copyOf(properties)
                ));
            });

            var fixSpans = createFixSpans(builder.fixes);
            schemas[i] = new OptimizedSchema(
                    builder.id, Map.copyOf(children), mapFixVersions(fixSpans.first()),
                    fixSpans.first(), fixSpans.second(),
                    List.copyOf(builder.properties)
            );
        }

        // Now we need to go OR the relevant versions of property types
        // This is a little tricky because types can be recursive.
        // Just brute force it by repeating until nothing changes...
        boolean changed;
        do {
            changed = false;
            for (var schema : schemas) {
                if (schema == null) continue;

                // OR the children
                for (var child : schema.idMap().values()) {
                    changed |= orBitSets(schema.relevantVersions(), child.relevantVersions());

                    for (var property : child.properties()) {
                        var propertySchema = schemas[property.getType().id()];
                        if (propertySchema == null) continue;

                        changed |= orBitSets(child.relevantVersions(), propertySchema.relevantVersions());
                    }
                }

                // OR the properties
                for (var property : schema.properties()) {
                    var propertySchema = schemas[property.getType().id()];
                    if (propertySchema == null) continue;

                    changed |= orBitSets(schema.relevantVersions(), propertySchema.relevantVersions());
                }
            }
        } while (changed);

        state.set(2);
    }

    private static @NotNull BitSet mapFixVersions(@NotNull Int2IntMap builder) {
        var relevantVersions = new BitSet(maxVersion() + 1);
        for (var version : builder.keySet()) relevantVersions.set(version);
        return relevantVersions;
    }

    // for some reason annotating this with @NotNull causes the compiler to just explode
    private static Pair<Int2IntMap, DataFix[]> createFixSpans(List<Pair<Integer, DataFix>> allFixes) {
        var sortedFixes = new ArrayList<>(allFixes);
        sortedFixes.sort(Comparator.comparingInt(Pair::first)); // Handles subversions by int natural order

        var spanMap = new Int2IntOpenHashMap();
        spanMap.defaultReturnValue(-1);
        var fixes = new DataFix[allFixes.size()];

        int version = -1, start = 0;
        for (int i = 0; i < fixes.length; i++) {
            var fix = sortedFixes.get(i);
            fixes[i] = fix.second();

            int fixVersion = fix.first() >> 8; // drop subversion
            if (version != fixVersion) {
                if (version != -1)
                    spanMap.put(version, (start << 16) | (i - start));
                version = fixVersion;
                start = i;
            }

        }
        if (version != -1) {
            spanMap.put(version, (start << 16) | (fixes.length - start));
        }

        return Pair.of(spanMap, fixes);
    }

    private static boolean orBitSets(BitSet a, BitSet b) {
        int aSize = a.size();
        a.or(b);
        return aSize != a.size();
    }

    // Upgrade methods

    public static <T> T upgrade(@NotNull DataType dataType, @NotNull Transcoder<T> coder, T value, int fromVersion, int toVersion) {
        var input = coder.convertTo(TranscoderValue.INSTANCE, value).orElseThrow();
        var result = upgrade(dataType, input, fromVersion, toVersion);
        return TranscoderValue.INSTANCE.convertTo(coder, result).orElseThrow();
    }

    public static Value upgrade(DataType dataType, Value value, int fromVersion, int toVersion) {
        fromVersion = Math.clamp(fromVersion, DataFixer.minVersion(), DataFixer.maxVersion());
        toVersion = Math.clamp(toVersion, DataFixer.minVersion(), DataFixer.maxVersion());
        if (fromVersion >= toVersion) return value;

        var typeSchema = DataFixer.schemas[dataType.id()];
        if (typeSchema == null) return value;

        // We can do a fancier fastpath for schemas with no children & no properties.
        if (typeSchema.oneshot()) return oneshotFastpath(typeSchema, value, fromVersion, toVersion);

        // Determine the initial schema to use, if this is an id mapped schema then the target may
        // be different.
        boolean isIdMapped = !typeSchema.idMap().isEmpty();
        String lastId = isIdMapped ? value.get("id").as(String.class, "") : null;
        var schema = isIdMapped ? typeSchema.idMap().getOrDefault(lastId, typeSchema) : typeSchema;

        for (int version = schema.relevantVersions().nextSetBit(fromVersion + 1);
             version >= 0 && version <= toVersion;
             version = schema.relevantVersions().nextSetBit(version + 1)
        ) {
            if (version == Integer.MAX_VALUE) break; // or (i+1) would overflow

            int fixSpan = schema.versionToFixSpan().get(version);
            if (fixSpan != -1) {
                int startIndex = fixSpan >> 16, count = fixSpan & 0xFF;
                for (int i = 0; i < count; i++) {
                    var result = schema.fixes()[startIndex + i].fix(value);
                    if (result != null) value = result;
                }
            }

            // If this is an id mapped schema its possible that a fix has changed the ID of the schema,
            // in which case we need to find the current schema.
            if (isIdMapped) {
                var id = value.get("id").as(String.class, "");
                if (!Objects.equals(lastId, id)) {
                    lastId = id;
                    schema = typeSchema.idMap().getOrDefault(lastId, typeSchema);
                    if (schema == null) break; // no more fixes for this ID
                }
            }

            if (!schema.properties().isEmpty()) {
                int innerVersion = version - 1;
                for (var property : schema.properties()) {
                    forEachAtPath(value, property.path(), 0, v ->
                            upgrade(property.getType(), v, innerVersion, innerVersion + 1));
                }
            }
        }

        return value;
    }

    private static Value oneshotFastpath(OptimizedSchema schema, Value value, int fromVersion, int toVersion) {
        int firstRelevantVersion = schema.relevantVersions().nextSetBit(fromVersion + 1);
        int lastRelevantVersion = schema.relevantVersions().previousSetBit(toVersion);
        if (firstRelevantVersion == -1 || lastRelevantVersion == -1) return value;

        int startIndex = schema.versionToFixSpan().get(firstRelevantVersion) >> 16;
        int endIndex = schema.versionToFixSpan().get(lastRelevantVersion);
        endIndex = (endIndex >> 16) + (endIndex & 0xFF);

        var fixes = schema.fixes();
        for (int i = startIndex; i < endIndex; i++) {
            var result = fixes[i].fix(value);
            if (result != null) value = result;
        }

        return value;
    }

    private static Value forEachAtPath(Value parent, @NotNull String[] path, int i, DataFix fix) {
        if (i >= path.length) {
            // A small caveat of "extend"/zero path to modify the root object is its invalid
            // to replace the entire root object. For example a datafix for entity equipment may not
            // replace the entire entity.
            return fix.fix(parent);
        }

        if (path[i].equals("*")) {
            parent.forEachEntry((id, entry) -> {
                var result = forEachAtPath(entry, path, i + 1, fix);
                if (result != null) {
                    parent.put(id, result);
                }
            });

            return null;
        }

        var value = parent.get(path[i]);
        if (value.isNull()) return null;
        if (value.isListLike()) {
            for (int li = 0; li < value.size(0); li++) {
                var result = forEachAtPath(value.get(li), path, i + 1, fix);
                if (result != null) value.put(li, result);
            }
            return null;
        }

        var result = forEachAtPath(value, path, i + 1, fix);
        if (result != null) parent.put(path[i], result);
        return null;
    }


    // Internal stuff :)

    static @NotNull DataTypeBuilder builderFor(@NotNull DataType dataType) {
        if (state.get() != 0) throw new IllegalArgumentException("DataFixer is already built.");
        return builders.computeIfAbsent(dataType.id(), _ -> new DataTypeBuilder(dataType.name()));
    }

    static @NotNull DataTypeBuilder builderFor(@NotNull DataType dataType, @NotNull String id) {
        return builderFor(dataType).getOrCreate(id);
    }


    // Old stuff to delete.


    public static int minVersion() {
        return minVersion;
    }

    public static int maxVersion() {
        return maxVersion;
    }

    static {
        addFixVersions(List.of( // 0xxx
                V99::new,
                V100::new,
                V101::new,
                V102::new,
                V105::new,
                V107::new,
                V108::new,
                V109::new,
                V110::new,
                V111::new,
                V113::new,
                V135::new,
                V143::new,
                V147::new,
                V165::new,
                V501::new,
                V502::new,
                V700::new,
                V701::new,
                V702::new,
                V703::new,
                V704::new,
                V705::new,
                V804::new,
                V806::new,
                V808::new,
                V813::new,
                V820::new
        ));
        addFixVersions(List.of( // 1xxx
                V1125::new,
                V1450::new,
                V1451::new,
                V1451_2::new,
                V1451_3::new,
                V1451_4::new,
                V1451_5::new,
                V1451_6::new,
                V1456::new,
                V1458::new,
                V1460::new,
                V1470::new,
                V1474::new,
                V1475::new,
                V1480::new,
                V1484::new,
                V1486::new,
                V1487::new,
                V1488::new,
                V1490::new,
                V1494::new,
                V1500::new,
                V1510::new,
                V1515::new,
                V1624::new,
                V1800::new,
                V1801::new,
                V1802::new,
                V1803::new,
                V1904::new,
                V1906::new,
                V1909::new,
                V1914::new,
                V1917::new,
                V1918::new,
                V1920::new,
                V1928::new,
                V1929::new,
                V1931::new,
                V1948::new,
                V1953::new,
                V1955::new,
                V1963::new
        ));
        addFixVersions(List.of( // 2xxx
                V2100::new,
                V2209::new,
                V2501::new,
                V2502::new,
                V2503::new,
                V2505::new,
                V2508::new,
                V2509::new,
                V2511::new,
                V2511_1::new,
                V2514::new,
                V2516::new,
                V2518::new,
                V2519::new,
                V2522::new,
                V2523::new,
                V2528::new,
                V2529::new,
                V2531::new,
                V2533::new,
                V2535::new,
                V2552::new,
                V2553::new,
                V2568::new,
                V2571::new,
                V2679::new,
                V2680::new,
                V2684::new,
                V2686::new,
                V2688::new,
                V2690::new,
                V2691::new,
                V2700::new,
                V2702::new,
                V2704::new,
                V2707::new,
                V2717::new,
                V2838::new,
                V2843::new
        ));
        addFixVersions(List.of( // 3xxx
                V3076::new,
                V3078::new,
                V3081::new,
                V3082::new,
                V3083::new,
                V3086::new,
                V3087::new,
                V3090::new,
                V3093::new,
                V3094::new,
                V3097::new,
                V3202::new,
                V3203::new,
                V3204::new,
                V3209::new,
                V3322::new,
                V3325::new,
                V3326::new,
                V3327::new,
                V3328::new,
                V3438::new,
                V3439::new,
                V3447::new,
                V3448::new,
                V3564::new,
                V3568::new,
                V3682::new,
                V3683::new,
                V3685::new,
                V3689::new,
                V3692::new,
                V3799::new,
                V3800::new,
                V3803::new,
                V3807::new,
                V3808::new,
                V3808_1::new,
                V3808_2::new,
                V3809::new,
                V3812::new,
                V3813::new,
                V3814::new,
                V3816::new,
                V3818::new,
                V3818_1::new,
                V3818_2::new,
                V3818_3::new,
                V3818_5::new,
                V3818_6::new,
                V3820::new,
                V3825::new,
                V3833::new,
                V3938::new,
                V3945::new
        ));
        addFixVersions(List.of( // 4xxx
                V4054::new,
                V4055::new,
                V4059::new,
                V4064::new,
                V4067::new,
                V4068::new,
                V4070::new,
                V4071::new,
                V4081::new,
                V4173::new,
                V4175::new,
                V4181::new,
                V4187::new,
                V4290::new,
                V4291::new,
                V4292::new,
                V4293::new,
                V4294::new,
                V4297::new,
                V4299::new,
                V4300::new,
                V4301::new,
                V4302::new,
                V4303::new,
                V4305::new,
                V4306::new,
                V4307::new,
                V4314::new,
                V4321::new,
                V4531::new,
                V4532::new,
                V4533::new,
                V4535::new,
                V4541::new,
                V4543::new
        ));
    }
}
