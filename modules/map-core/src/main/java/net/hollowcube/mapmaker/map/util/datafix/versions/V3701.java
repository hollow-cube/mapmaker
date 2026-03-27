package net.hollowcube.mapmaker.map.util.datafix.versions;

import com.google.auto.service.AutoService;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.ExternalDataFix;
import net.hollowcube.datafix.fixes.BlockEntityRenameFix;
import net.hollowcube.mapmaker.map.util.datafix.HCDataTypes;

@AutoService(ExternalDataFix.class)
public class V3701 extends DataVersion implements ExternalDataFix {
    public V3701() {
        super(3701);

        addReference(HCDataTypes.CHUNK, field -> field.list("entities", DataTypes.ENTITY));

        addReference(DataTypes.BLOCK_ENTITY, "mapmaker:checkpoint_plate");
        addReference(DataTypes.BLOCK_ENTITY, "mapmaker:status_plate");
        addReference(DataTypes.BLOCK_ENTITY, "mapmaker:finish_plate");
        addReference(DataTypes.BLOCK_ENTITY, "mapmaker:bounce_pad");

        // Honestly I have no idea why we ever had player_head and it worked. the game seems to think its skull
        // and I can't find a datafix which does this remapping (maybe i missed it).
        // In any case, convert now and the 1.20.5 snapshot fixes will remap `SkullOwner` to `profile`
        addFix(DataTypes.BLOCK_ENTITY, new BlockEntityRenameFix("minecraft:player_head", "minecraft:skull"));
    }

}
