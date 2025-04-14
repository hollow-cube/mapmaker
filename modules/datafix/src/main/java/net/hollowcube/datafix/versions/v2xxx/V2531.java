package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockStatePropertiesFix;
import net.hollowcube.datafix.util.Value;

public class V2531 extends DataVersion {
    public V2531() {
        super(2531);

        addFix(DataTypes.BLOCK_STATE, new BlockStatePropertiesFix(
                "minecraft:redstone_wire", V2531::fixRedstoneWireConnections));
    }

    private static void fixRedstoneWireConnections(Value properties) {
        var east = properties.getValue("east") instanceof String s ? s : "none";
        var west = properties.getValue("west") instanceof String s ? s : "none";
        var north = properties.getValue("north") instanceof String s ? s : "none";
        var south = properties.getValue("south") instanceof String s ? s : "none";

        boolean isEastWest = isConnected(east) || isConnected(west);
        boolean isNorthSouth = isConnected(north) || isConnected(south);

        properties.put("east", !isConnected(east) && !isNorthSouth ? "side" : east);
        properties.put("west", !isConnected(west) && !isNorthSouth ? "side" : west);
        properties.put("north", !isConnected(north) && !isEastWest ? "side" : north);
        properties.put("south", !isConnected(south) && !isEastWest ? "side" : south);
    }

    private static boolean isConnected(String value) {
        return !"none".equals(value);
    }
}
