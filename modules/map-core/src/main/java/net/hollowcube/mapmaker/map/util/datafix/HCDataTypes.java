package net.hollowcube.mapmaker.map.util.datafix;

import net.hollowcube.datafix.DataType;
import net.kyori.adventure.key.Key;

public final class HCDataTypes {

    // Entities are stored as NBT using the vanilla Entity format with a handful of extensions for extra data.
    // Extensions are registered on the original MCTypeRegistry.ENTITY (and entities should be converted as such)

    public static final DataType WORLD = DataType.dataType(Key.key("mapmaker:world")); // Polar world user data
    public static final DataType CHUNK = DataType.dataType(Key.key("mapmaker:chunk")); // Polar chunk user data

    public static final DataType EDIT_STATE = DataType.dataType(Key.key("mapmaker:edit_state")); // Save state for an editing world
    public static final DataType PLAY_STATE = DataType.dataType(Key.key("mapmaker:play_state")); // Save state for a playing world

    private HCDataTypes() {
    }
}
