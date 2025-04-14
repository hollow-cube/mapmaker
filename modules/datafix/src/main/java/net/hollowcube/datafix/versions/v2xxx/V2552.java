package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BiomeRenameFix;

public class V2552 extends DataVersion {
    public V2552() {
        super(2552);

        addFix(DataTypes.BIOME_NAME, new BiomeRenameFix("minecraft:nether", "minecraft:nether_wastes"));
    }
}
