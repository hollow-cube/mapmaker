package net.hollowcube.mapmaker.map.util.datafix.versions;

import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.itemstack.DataWalkerItems;
import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.util.datafix.HCTypeRegistry;
import net.hollowcube.mapmaker.map.util.datafix.HCVersions;
import net.hollowcube.mapmaker.map.util.datafix.walkers.DataWalkerMapPaths;
import net.hollowcube.mapmaker.map.util.datafix.walkers.MapPathWalker;

@AutoService(DataFix.class)
public class V3838 implements DataFix {
    private static final int VERSION = HCVersions.V1_20_5_HC1;

    @Override
    public void register() {
        HCTypeRegistry.BLOCK_ENTITY.addWalker(VERSION, "minecraft:checkpoint_plate", new MapPathWalker("items", new DataWalkerItems("item1", "item2", "item3")));
        HCTypeRegistry.BLOCK_ENTITY.addWalker(VERSION, "minecraft:status_plate", new MapPathWalker("items", new DataWalkerItems("item1", "item2", "item3")));

        // Both states 'added' in this version
        HCTypeRegistry.EDIT_STATE.addStructureWalker(VERSION, new DataWalkerMapPaths<>(MCTypeRegistry.ITEM_STACK, "inventory"));
        HCTypeRegistry.PLAY_STATE.addStructureWalker(VERSION, new DataWalkerMapPaths<>(MCTypeRegistry.FLAT_BLOCK_STATE, "ghostBlocks"));
    }
}
