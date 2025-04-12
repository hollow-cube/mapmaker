package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V1510 extends DataVersion {
    public V1510() {
        super(1510);

        renameReference(DataType.ENTITY, "minecraft:commandblock_minecart", "minecraft:command_block_minecart");
        renameReference(DataType.ENTITY, "minecraft:ender_crystal", "minecraft:end_crystal");
        renameReference(DataType.ENTITY, "minecraft:snowman", "minecraft:snow_golem");
        renameReference(DataType.ENTITY, "minecraft:evocation_illager", "minecraft:evoker");
        renameReference(DataType.ENTITY, "minecraft:evocation_fangs", "minecraft:evoker_fangs");
        renameReference(DataType.ENTITY, "minecraft:illusion_illager", "minecraft:illusioner");
        renameReference(DataType.ENTITY, "minecraft:vindication_illager", "minecraft:vindicator");
        renameReference(DataType.ENTITY, "minecraft:villager_golem", "minecraft:iron_golem");
        renameReference(DataType.ENTITY, "minecraft:xp_orb", "minecraft:experience_orb");
        renameReference(DataType.ENTITY, "minecraft:xp_bottle", "minecraft:experience_bottle");
        renameReference(DataType.ENTITY, "minecraft:eye_of_ender_signal", "minecraft:eye_of_ender");
        renameReference(DataType.ENTITY, "minecraft:fireworks_rocket", "minecraft:firework_rocket");
    }
}
