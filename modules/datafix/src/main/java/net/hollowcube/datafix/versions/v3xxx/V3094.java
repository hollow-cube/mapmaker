package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V3094 extends DataVersion {
    private static final String[] INSTRUMENTS = new String[]{
            "minecraft:ponder_goat_horn",
            "minecraft:sing_goat_horn",
            "minecraft:seek_goat_horn",
            "minecraft:feel_goat_horn",
            "minecraft:admire_goat_horn",
            "minecraft:call_goat_horn",
            "minecraft:yearn_goat_horn",
            "minecraft:dream_goat_horn"
    };

    public V3094() {
        super(3094);

        addFix(DataType.ITEM_STACK, "minecraft:goat_horn", V3094::fixGoatHornInstrument);
    }

    private static Value fixGoatHornInstrument(Value itemStack) {
        var tag = itemStack.get("tag");
        if (!tag.isMapLike()) return null;

        int soundVariant = tag.remove("SoundVariant").as(Number.class, 0).intValue();
        tag.put("instrument", INSTRUMENTS[Math.clamp(0, INSTRUMENTS.length - 1, soundVariant)]);
        return null;
    }
}
