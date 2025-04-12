package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class V704 extends DataVersion {
    private static final Map<String, String> BLOCK_ENTITY_IDS = Map.ofEntries(
            Map.entry("Airportal", "minecraft:end_portal"),
            Map.entry("Banner", "minecraft:banner"),
            Map.entry("Beacon", "minecraft:beacon"),
            Map.entry("Cauldron", "minecraft:brewing_stand"),
            Map.entry("Chest", "minecraft:chest"),
            Map.entry("Comparator", "minecraft:comparator"),
            Map.entry("Control", "minecraft:command_block"),
            Map.entry("DLDetector", "minecraft:daylight_detector"),
            Map.entry("Dropper", "minecraft:dropper"),
            Map.entry("EnchantTable", "minecraft:enchanting_table"),
            Map.entry("EndGateway", "minecraft:end_gateway"),
            Map.entry("EnderChest", "minecraft:ender_chest"),
            Map.entry("FlowerPot", "minecraft:flower_pot"),
            Map.entry("Furnace", "minecraft:furnace"),
            Map.entry("Hopper", "minecraft:hopper"),
            Map.entry("MobSpawner", "minecraft:mob_spawner"),
            Map.entry("Music", "minecraft:noteblock"),
            Map.entry("Piston", "minecraft:piston"),
            Map.entry("RecordPlayer", "minecraft:jukebox"),
            Map.entry("Sign", "minecraft:sign"),
            Map.entry("Skull", "minecraft:skull"),
            Map.entry("Structure", "minecraft:structure_block"),
            Map.entry("Trap", "minecraft:dispenser")
    );

    public V704() {
        super(704);

        addFix(DataType.BLOCK_ENTITY, this::fixBlockEntityId);
        BLOCK_ENTITY_IDS.forEach((oldId, newId) ->
                renameReference(DataType.BLOCK_ENTITY, oldId, newId));

    }

    private @NotNull Map<String, Object> fixBlockEntityId(@NotNull Map<String, Object> blockEntity) {
        if (blockEntity.get("id") instanceof String id)
            blockEntity.put("id", BLOCK_ENTITY_IDS.getOrDefault(id, id));
        return blockEntity;
    }
}
