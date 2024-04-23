package net.hollowcube.mapmaker.map.util.datafix.versions;

import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.util.datafix.HCTypeRegistry;
import net.hollowcube.mapmaker.map.util.datafix.HCVersions;

@AutoService(DataFix.class)
public class V3701 implements DataFix {
    private static final int VERSION = HCVersions.V1_20_4_HC1;

    @Override
    public void register() {
        HCTypeRegistry.CHUNK.addStructureWalker(VERSION, (data, fromVersion, toVersion) -> {
            WalkerUtils.convertList(MCTypeRegistry.ENTITY, data, "entities", fromVersion, toVersion);

            return null;
        });
    }
}
