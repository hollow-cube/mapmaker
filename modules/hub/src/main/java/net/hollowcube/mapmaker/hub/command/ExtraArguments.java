package net.hollowcube.mapmaker.hub.command;

import net.hollowcube.mapmaker.model.PlayerData;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;

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
    public static Argument<Integer> MapSlot(boolean showAvailable) {
         return ArgumentType.Integer("slot")
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
                     value -= 1;
                     if (value < 0 || value > PlayerData.MAX_MAP_SLOTS)
                         throw new ArgumentSyntaxException("Invalid map slot", String.valueOf(value), ERR_INVALID_MAP_SLOT);
                     return value;
                 });
    }

}
