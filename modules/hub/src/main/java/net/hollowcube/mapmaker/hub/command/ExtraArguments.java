package net.hollowcube.mapmaker.hub.command;

import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public final class ExtraArguments {
    private ExtraArguments() {
    }

    public static final int ERR_INVALID_MAP_SLOT = 1;

    /**
     * An integer argument which represents a map slot, will only include
     * completion values for slots which are/not occupied depending on the `used` arg.
     * <p>
     * The slot is autocompleted using a 1 indexed number, but returned zero indexed.
     */
    public static Argument<Integer> MapSlot(@NotNull String id, boolean showAvailable) {
        return ArgumentType.Integer(id)
                .min(1).max(PlayerData.MAX_MAP_SLOTS + 1)
                .setSuggestionCallback((sender, context, suggestion) -> {
                    if (!(sender instanceof Player player)) return;

                    var playerData = PlayerData.fromPlayer(player);
                    for (int i = 0; i < PlayerData.MAX_MAP_SLOTS; i++) {
                        if (showAvailable == (playerData.getMapSlot(i) != null)) {
                            var entry = new SuggestionEntry(String.valueOf(i + 1));
                            suggestion.addEntry(entry);
                        }
                    }
                })
                .map(value -> {
                    //todo check players slots to throw error for filled slot
                    value -= 1;
                    if (value < 0 || value > PlayerData.MAX_MAP_SLOTS)
                        throw new ArgumentSyntaxException("Invalid map slot", String.valueOf(value), ERR_INVALID_MAP_SLOT);
                    return value;
                });
    }

    public static final int MASK_ID = 1;
    public static final int MASK_SLOT = 2;
    public static final int MASK_PERSONAL_WORLD = 4;
    public static final int MASK_PUBLISHED_ID = 8;

    // If null is returned, the command should be exited immediately
    public static @NotNull Argument<@Nullable MapData> Map(@NotNull String id, int mask) {
        return ArgumentType.String(id)
//                .setSuggestionCallback((sender, context, suggestion) -> {
//
//                })
                .map((sender, value) -> {
                    if (!(sender instanceof Player player))
                        throw new ArgumentSyntaxException("Invalid map", value, ERR_INVALID_MAP_SLOT); //todo a more descriptive error
                    var playerData = PlayerData.fromPlayer(player);

                    try { // Try to parse as a slot
                        var slot = Integer.parseInt(value) - 1;

                        // Personal world
                        if (slot == PlayerData.MAX_MAP_SLOTS + 1) {
                            if ((mask & MASK_PERSONAL_WORLD) == 0) // Personal worlds are not enabled
                                throw new ArgumentSyntaxException("Invalid map", String.valueOf(slot), ERR_INVALID_MAP_SLOT);

                            throw new UnsupportedOperationException("personal worlds not supported");
                        }

                        // Regular slot
                        if (slot >= 0 && slot < PlayerData.MAX_MAP_SLOTS) {
                            if ((mask & MASK_SLOT) == 0) // Filled slots are not enabled
                                throw new ArgumentSyntaxException("Invalid map", String.valueOf(slot), ERR_INVALID_MAP_SLOT);

                            var state = playerData.getSlotState(slot);
                            if (state == PlayerData.SLOT_STATE_LOCKED)
                                throw new ArgumentSyntaxException("Locked slot", String.valueOf(slot), ERR_INVALID_MAP_SLOT);
                            if (state == PlayerData.SLOT_STATE_OPEN)
                                throw new ArgumentSyntaxException("Empty slot", String.valueOf(slot), ERR_INVALID_MAP_SLOT);

                            var mapId = Objects.requireNonNull(playerData.getMapSlot(slot));
                            return HubServer.StaticAbuse.instance.mapService().getMap(playerData.getId(), mapId);
                        }

                        // Any other number is invalid for sure
                        throw new ArgumentSyntaxException("Invalid map", String.valueOf(slot), ERR_INVALID_MAP_SLOT);
                    } catch (NumberFormatException ignored) {
                    }

                    try {
                        var mapId = UUID.fromString(value).toString();
                        if ((mask & MASK_ID) == 0) // Ids are not enabled
                            throw new ArgumentSyntaxException("Invalid map", value, ERR_INVALID_MAP_SLOT);

                        return HubServer.StaticAbuse.instance.mapService().getMap(playerData.getId(), mapId);
                    } catch (IllegalArgumentException ignored) {
                    }

                    //todo publishedId

                    throw new ArgumentSyntaxException("Invalid map", value, ERR_INVALID_MAP_SLOT);
                });
    }

}
