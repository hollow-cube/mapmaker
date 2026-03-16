package net.hollowcube.datafix.versions.v1xxx;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;

public class V1450 extends DataVersion {
    private static final @Nullable Value[] BLOCK_STATE_BY_LEGACY_ID = new Value[4096];
    private static final Object2IntMap<Value> LEGACY_ID_BY_LEGACY_BLOCK_STATE = new Object2IntOpenHashMap<>();
    private static final Object2IntMap<String> LEGACY_ID_BY_NAME = new Object2IntOpenHashMap<>();
    private static final Value AIR = Value.wrap("minecraft:air");

    public V1450() {
        super(1450);

        // TODO: this is the very gross block state upgrader
        addFix(DataTypes.BLOCK_STATE, V1450::fixUpgradeBlockState);
    }

    public static Value upgradeBlockId(int id) {
        if (id >= 0 && id < BLOCK_STATE_BY_LEGACY_ID.length) {
            Value blockState = BLOCK_STATE_BY_LEGACY_ID[id];
            if (blockState != null) return blockState;
        }
        return AIR;
    }

    public static Value upgradeBlockName(String name) {
        int legacyId = LEGACY_ID_BY_NAME.getInt(name);
        if (legacyId >= 0 && legacyId < BLOCK_STATE_BY_LEGACY_ID.length) {
            Value blockState = BLOCK_STATE_BY_LEGACY_ID[legacyId];
            if (blockState != null) return blockState.get("Name");
        }
        return Value.wrap(name);
    }

    public static @Nullable Value getBlockState(int legacyId, int legacyData) {
        int index = (legacyId << 4) | (legacyData & 0xF);
        if (index >= 0 && index < BLOCK_STATE_BY_LEGACY_ID.length)
            return BLOCK_STATE_BY_LEGACY_ID[index];
        return null;
    }

    private static @Nullable Value fixUpgradeBlockState(Value blockState) {
        int legacyId = LEGACY_ID_BY_LEGACY_BLOCK_STATE.getInt(blockState);
        if (legacyId >= 0 && legacyId < 4096)
            return BLOCK_STATE_BY_LEGACY_ID[legacyId];
        return null;
    }

    static {
        Arrays.fill(BLOCK_STATE_BY_LEGACY_ID, Value.NULL);
        LEGACY_ID_BY_LEGACY_BLOCK_STATE.defaultReturnValue(-1);
        LEGACY_ID_BY_NAME.defaultReturnValue(-1);

        var gson = new Gson();

        var path = "/net/hollowcube/datafix/v1450/block_state_by_legacy_id.json";
        try (var is = V1450.class.getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("Resource not found: " + path);
            var blocks = gson.fromJson(new InputStreamReader(is), JsonObject.class);
            var sortedEntries = blocks.entrySet().stream()
                .map(entry -> Map.entry(Integer.parseInt(entry.getKey()), entry.getValue().getAsString()))
                .sorted(Map.Entry.comparingByKey())
                .toList();
            for (int i = 0; i < sortedEntries.size(); i++) {
                var entry = sortedEntries.get(i);

                if (!entry.getValue().startsWith("null")) {
                    BLOCK_STATE_BY_LEGACY_ID[entry.getKey()] = parseBlockState(entry.getValue());
                }

                // Fill gap between previous with previous
                if (i > 0) {
                    var prevId = sortedEntries.get(i - 1).getKey();
                    for (int j = prevId + 1; j < entry.getKey(); j++) {
                        BLOCK_STATE_BY_LEGACY_ID[j] = BLOCK_STATE_BY_LEGACY_ID[prevId];
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        path = "/net/hollowcube/datafix/v1450/legacy_id_by_legacy_block_state.json";
        try (var is = V1450.class.getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("Resource not found: " + path);
            var blocks = gson.fromJson(new InputStreamReader(is), JsonObject.class);
            for (var entry : blocks.entrySet()) {
                LEGACY_ID_BY_LEGACY_BLOCK_STATE.put(parseBlockState(entry.getKey()), entry.getValue().getAsInt());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        path = "/net/hollowcube/datafix/v1450/legacy_id_by_name.json";
        try (var is = V1450.class.getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("Resource not found: " + path);
            var blocks = gson.fromJson(new InputStreamReader(is), JsonObject.class);
            for (var entry : blocks.entrySet()) {
                LEGACY_ID_BY_NAME.put(entry.getKey().intern(), entry.getValue().getAsInt());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Value parseBlockState(@NotNull String state) {
        int nbtIndex = state.indexOf("[");
        if (nbtIndex == 0) {
            return Value.NULL;
        } else if (nbtIndex == -1) {
            var block = Value.emptyMap();
            block.put("Name", state.intern());
            return block;
        } else if (!state.endsWith("]")) {
            return Value.NULL;
        } else {
            var block = Value.emptyMap();
            block.put("Name", state.substring(0, nbtIndex).intern());
            block.put("Properties", parseBlockProperties(state.substring(nbtIndex + 1, state.length() - 1)));
            return block;
        }
    }

    private static Value parseBlockProperties(String propertyList) {
        Value properties = Value.emptyMap();
        int start = 0;
        int index = 0;
        while (index < propertyList.length()) {
            if (propertyList.charAt(index) == ',' || index == propertyList.length() - 1) {
                final int equalIndex = propertyList.indexOf('=', start);
                if (equalIndex != -1) {
                    final String key = propertyList.substring(start, equalIndex);
                    final String value = propertyList.substring(equalIndex + 1, index + 1);
                    properties.put(key.intern(), value.intern());
                }
                start = index + 1;
            }
            index++;
        }

        return properties;
    }
}
