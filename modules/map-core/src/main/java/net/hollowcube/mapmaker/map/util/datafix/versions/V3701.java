package net.hollowcube.mapmaker.map.util.datafix.versions;

import ca.spottedleaf.dataconverter.minecraft.converters.tileentity.ConverterAbstractTileEntityRename;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.util.datafix.HCTypeRegistry;
import net.hollowcube.mapmaker.map.util.datafix.HCVersions;

import java.util.Map;

@AutoService(DataFix.class)
public class V3701 implements DataFix {
    private static final int VERSION = HCVersions.V1_20_4_HC1;

    @Override
    public void register() {
        // Honestly I have no idea why we ever had player_head and it worked. the game seems to think its skull
        // and I can't find a datafix which does this remapping (maybe i missed it).
        // In any case, convert now and the 1.20.5 snapshot fixes will remap `SkullOwner` to `profile`
        ConverterAbstractTileEntityRename.register(VERSION, Map.of("minecraft:player_head", "minecraft:skull")::get);

        HCTypeRegistry.CHUNK.addStructureWalker(VERSION, (data, fromVersion, toVersion) -> {
            WalkerUtils.convertList(MCTypeRegistry.ENTITY, data, "entities", fromVersion, toVersion);

            return null;
        });

        // mapmaker:checkpoint_plate is a simple block entity
        // mapmaker:status_plate is a simple block entity
        // mapmaker:finish_plate is a simple block entity
        // mapmaker:bounce_pad is a simple block entity
    }
}
