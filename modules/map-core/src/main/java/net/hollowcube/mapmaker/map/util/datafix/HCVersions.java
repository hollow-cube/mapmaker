package net.hollowcube.mapmaker.map.util.datafix;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;

public class HCVersions {
    // Mojang always offsets by 100 when bumping the (release) protocol version, so we can fill in the versions between.

    public static final int V1_20_4_HC1 = MCVersions.V1_20_4 + 1; // 3701
}
