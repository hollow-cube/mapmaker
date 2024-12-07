package net.hollowcube.mapmaker.map.util.datafix;

import ca.spottedleaf.dataconverter.minecraft.MCVersions;

public class HCVersions {
    // Mojang always offsets by 100 when bumping the (release) protocol version, so we can fill in the versions between.

    public static final int V1_20_4_HC1 = MCVersions.V1_20_4 + 1; // 3701
    public static final int V1_20_5_HC1 = MCVersions.V1_20_6 + 1; // 3839
    public static final int V1_21_HC1 = MCVersions.V1_21_1 + 1; // 3954
    public static final int V1_21_3_HC1 = MCVersions.V1_21_3 + 1;
    public static final int V1_21_4_HC1 = MCVersions.V1_21_4 + 1;
}
