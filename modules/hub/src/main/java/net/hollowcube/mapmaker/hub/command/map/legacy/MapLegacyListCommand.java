package net.hollowcube.mapmaker.hub.command.map.legacy;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument2;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MapLegacyListCommand extends CommandDsl {
    private final Argument2<String> playerArg = Argument2.Word("player");

    private final MapService mapService;

    public MapLegacyListCommand(@NotNull MapService mapService, @NotNull PermManager permManager) {
        super("list");
        this.mapService = mapService;

        var listAnyPerm = permManager.createPlatformCondition2(PlatformPerm.MAP_ADMIN);

        addSyntax(playerOnly(this::listLegacyMaps));
//        addSyntax(listAnyPerm, playerOnly(this::listLegacyMaps), playerArg);
    }

    private void listLegacyMaps(@NotNull Player player, @NotNull CommandContext context) {
        var target = player.getUuid().toString();
        if (context.has(playerArg)) target = context.get(playerArg);

        try {
            var playerData = PlayerDataV2.fromPlayer(player);
            var legacyMaps = mapService.getLegacyMaps(playerData.id(), target);

            if (legacyMaps.isEmpty()) {
                player.sendMessage(Component.translatable("command.map.legacy.list.no_maps"));
                return;
            }

            player.sendMessage(Component.translatable("command.map.legacy.list.header"));
            for (var map : legacyMaps) {
                player.sendMessage(Component.translatable("command.map.legacy.list.entry", Component.text(map.name()), Component.text(map.id())));
            }
        } catch (Exception e) {
            player.sendMessage(Component.translatable("command.map.legacy.list.failure"));
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }

}
