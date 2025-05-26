package net.hollowcube.mapmaker.map.util.datafix;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.NotNull;

public class V4326 extends DataVersion {

    public V4326() {
        super(4326);

        addFix(DataTypes.BLOCK_ENTITY, "mapmaker:checkpoint_plate", V4326::updateCheckpointPlateBlockEntity);
        addFix(DataTypes.BLOCK_ENTITY, "mapmaker:status_plate", V4326::updateStatusPlateBlockEntity);
        addFix(DataTypes.ENTITY, "minecraft:marker", V4326::updateEffectMarker);
        addFix(HCDataTypes.WORLD, V4326::updateWorldSpawnCheckpoint);
        addFix(HCDataTypes.PLAY_STATE, V4326::fixPlayStateToActionMap);
    }

    private static Value updateCheckpointPlateBlockEntity(@NotNull Value blockEntity) {
        updateCheckpointData(blockEntity, true);
        return null;
    }

    private static Value updateStatusPlateBlockEntity(@NotNull Value blockEntity) {
        updateCheckpointData(blockEntity, false);
        return null;
    }

    private static Value updateWorldSpawnCheckpoint(@NotNull Value worldData) {
        var spawnCheckpoint = worldData.get("spawn_checkpoint_effects");
        if (!spawnCheckpoint.isNull()) updateCheckpointData(spawnCheckpoint, true);
        return null;
    }

    private static Value updateEffectMarker(@NotNull Value entity) {
        var data = entity.get("data");
        var type = data.get("type").as(String.class, "");
        if ("mapmaker:checkpoint".equals(type)) {
            updateCheckpointData(data.get("checkpoint"), true);
        } else if ("mapmaker:status".equals(type)) {
            updateCheckpointData(data.get("status"), false);
        }
        return null;
    }

    private static Value fixPlayStateToActionMap(Value playState) {
        // ghostBlocks, history, pos left alone.

        playState.put("mapmaker:progress_index", playState.remove("progressIndex"));
        playState.put("mapmaker:timer", playState.remove("timeLimit"));
        playState.put("mapmaker:reset_height", playState.remove("resetHeight"));
        playState.put("mapmaker:potion_effects", playState.remove("potionEffects"));
        var lives = Value.emptyMap();
        lives.put("max", playState.remove("maxLives"));
        lives.put("value", playState.remove("lives"));
        if (lives.size(0) > 0)
            playState.put("mapmaker:lives", lives);
        var items = playState.remove("items"); // was {item1,item2,item3,elytra}
        if (playState.get("elytra").as(Boolean.class, false))
            playState.put("mapmaker:elytra", true);
        var newItems = Value.emptyMap();
        newItems.put("item0", fixItem(items.remove("item1")));
        newItems.put("item1", fixItem(items.remove("item2")));
        newItems.put("item2", fixItem(items.remove("item3")));
        if (newItems.size(0) > 0)
            playState.put("mapmaker:hotbar_items", newItems);
        playState.put("mapmaker:settings", playState.remove("settings"));

        var lastState = playState.get("lastState");
        if (!lastState.isNull()) fixPlayStateToActionMap(lastState);

        return playState;
    }

    private static Value fixItem(Value item) {
        var type = item.get("type").as(String.class, "");
        return switch (type) {
            case "firework_rocket" -> {
                var firework = Value.emptyMap();
                firework.put("item", "minecraft:firework_rocket");
                firework.put("amount", item.remove("quantity"));
                firework.put("duration", item.remove("duration").as(Number.class, 0).intValue() / 50);
                yield firework;
            }
            case "trident" -> {
                var trident = Value.emptyMap();
                trident.put("item", "minecraft:trident");
                trident.put("riptide", item.remove("riptideLevel"));
                yield trident;
            }
            default -> null;
        };
    }

    private static void updateCheckpointData(@NotNull Value container, boolean isCheckpoint) {
        var actions = Value.emptyList();

        container.remove("name"); // Gone
        var progressIndex = container.remove("progressIndex");
        if (!progressIndex.isNull()) {
            var action = Value.emptyMap();
            action.put("type", "mapmaker:progress_index");
            action.put("value", progressIndex);
            actions.put(action);
        }
        var timeLimit = container.remove("timeLimit").as(Number.class, 0).intValue();
        if (timeLimit > 0) {
            var action = Value.emptyMap();
            action.put("type", "mapmaker:timer");
            action.put("value", timeLimit / 50);
            actions.put(action);
        } else if (isCheckpoint) {
            // Historically checkpoints always reset your timer. Add a reset to account for this.
            var action = Value.emptyMap();
            action.put("type", "mapmaker:timer");
            actions.put(action);
        }
        var extraTime = container.remove("extraTime").as(Number.class, 0).intValue();
        if (extraTime > 0) {
            var action = Value.emptyMap();
            action.put("type", "mapmaker:timer");
            action.put("operation", "add");
            action.put("value", extraTime / 50);
            actions.put(action);
        }
        var resetHeight = container.remove("resetHeight");
        if (!resetHeight.isNull()) {
            var action = Value.emptyMap();
            action.put("type", "mapmaker:reset_height");
            action.put("value", resetHeight);
            actions.put(action);
        }
        var clearPotionEffects = container.remove("clearPotionEffects");
        if (!clearPotionEffects.isNull() && clearPotionEffects.as(Boolean.class, false)) {
            var action = Value.emptyMap();
            action.put("type", "mapmaker:remove_potion");
            actions.put(action);
        }
        var potionEffects = container.remove("potionEffects");
        if (!potionEffects.isNull() && potionEffects.size(0) > 0) {
            for (var effect : potionEffects) {
                effect.put("effect", effect.remove("type"));
                effect.put("type", "mapmaker:add_potion");
                effect.put("duration", effect.remove("duration").as(Number.class, 0).intValue() / 50);
                // level is OK
                actions.put(effect);
            }
        }
        var teleport = container.remove("teleport");
        if (!teleport.isNull()) {
            teleport.put("type", "mapmaker:teleport");
            actions.put(teleport);
        }
        var items = container.remove("items");
        if (!items.isNull()) {
            var elytra = items.remove("elytra");
            if (!elytra.isNull()) {
                var action = Value.emptyMap();
                action.put("type", elytra.as(Boolean.class, false)
                        ? "mapmaker:give_elytra" : "mapmaker:take_elytra");
                actions.put(action);
            }
            for (int i = 1; i <= 3; i++) {
                var item = items.remove("item" + i);
                if (!item.isNull()) {
                    var action = Value.emptyMap();
                    action.put("slot", i - 1);
                    var type = item.get("type").as(String.class, "remove");
                    if ("remove".equals(type)) {
                        action.put("type", "mapmaker:take_item");
                    } else {
                        action.put("type", "mapmaker:give_item");
                        action.put("item", type);
                        action.put("amount", item.remove("quantity"));
                        action.put("duration", item.remove("duration").as(Number.class, 0).intValue() / 50);
                        action.put("riptideLevel", item.remove("riptideLevel"));
                    }
                    actions.put(action);
                }
            }
        }
        var settings = container.remove("settings");
        settings.forEachEntry((key, value) -> {
            var enable = value.as(Boolean.class, false);
            var action = Value.emptyMap();
            action.put("type", enable ? "mapmaker:enable_setting" : "mapmaker:disable_setting");
            action.put("setting", key);
            actions.put(action);
        });

        if (actions.size(0) > 0)
            container.put("actions", actions);
    }
}
