package net.hollowcube.mapmaker.map.util.datafix.versions;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.util.datafix.HCVersions;

@AutoService(DataFix.class)
public class V3839 implements DataFix {
    private static final int VERSION = HCVersions.V1_20_5_HC1;

    @Override
    public void register() {
//        HCTypeRegistry.BLOCK_ENTITY.addWalker(VERSION, "minecraft:checkpoint_plate", new DataWalkerItems("items"));//todo
    }
}
