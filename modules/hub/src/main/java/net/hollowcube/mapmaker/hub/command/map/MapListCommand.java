package net.hollowcube.mapmaker.hub.command.map;

import net.hollowcube.mapmaker.hub.command.BaseHubCommand;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MapListCommand extends BaseHubCommand {
    private final ArgumentEntity playerArg = ArgumentType.Entity("player")
            .onlyPlayers(true)
            .singleEntity(true);

    private final MapService mapService;

    public MapListCommand(@NotNull MapService mapService) {
        super("list");
        this.mapService = mapService;

        addSyntax(wrap(this::listMapsSelf));
        addSyntax(wrap(this::listMapsOther), playerArg);
    }

    private void listMapsSelf(@NotNull Player player, @NotNull CommandContext context) {
        showMapList(player, PlayerDataV2.fromPlayer(player));
    }

    private void listMapsOther(@NotNull Player player, @NotNull CommandContext context) {
        var target = context.get(playerArg).findFirstPlayer(player);
        if (target == null) {
            player.sendMessage("Invalid player.");
            return;
        }

        //todo this only works for other online players, eventually it should work for offline players
        showMapList(player, PlayerDataV2.fromPlayer(target));
    }

    private void showMapList(@NotNull Player player, @NotNull PlayerDataV2 playerData) {
        for (var slot = 0; slot < PlayerDataV2.MAX_MAP_SLOTS; slot++) {
            var slotId = slot + 1;
            switch (playerData.getSlotState(slot)) {
                case FILLED -> {
                    var map = mapService.getMap(playerData.id(), Objects.requireNonNull(playerData.getMapSlot(slot)));
                    var message = Component.text("Slot " + slotId + " is '")
                            .append(Component.text(map.name())
                                    .hoverEvent(HoverEvent.showText(Component.text("Click to copy ID")))
                                    .clickEvent(ClickEvent.copyToClipboard(map.id())))
                            .append(Component.text("' "))
                            .append(Component.text("(edit)")
                                    .hoverEvent(HoverEvent.showText(Component.text("Click to edit")))
                                    .clickEvent(ClickEvent.runCommand("/map edit " + map.id())));
                    player.sendMessage(message);
                }
                case EMPTY -> player.sendMessage("Slot " + slotId + " is empty.");
                case LOCKED -> player.sendMessage("Slot " + slotId + " is locked.");
            }
        }
    }

}
