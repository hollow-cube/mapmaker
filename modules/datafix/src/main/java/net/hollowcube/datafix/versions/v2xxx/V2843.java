package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BiomeRenameFix;

public class V2843 extends DataVersion {
    public V2843() {
        super(2843);

        addFix(DataTypes.BIOME_NAME, new BiomeRenameFix(
                "minecraft:deep_warm_ocean", "minecraft:warm_ocean"
        ));
    }
}
