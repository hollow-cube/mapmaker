package net.hollowcube.mapmaker.hub.legacy;

import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.hub.legacy.LegacyMapService;
import net.hollowcube.mapmaker.model.PlayerData;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class LegacyCommand extends Command {
    private final HubServer server;
    private final LegacyMapService legacy;

    public LegacyCommand(@NotNull HubServer server) {
        super("legacy");
        this.server = server;
        this.legacy = server.legacyMapService();

        addSubcommand(new List());
        addSubcommand(new Import());
        addSubcommand(new Export());
    }

    //map legacy list 0444697c-a3b1-46e8-92d3-1d58e441741e
    private class List extends Command {
        private static final long PAGE_SIZE = 10;

        private final Argument<Integer> pageArg = ArgumentType.Integer("page").min(0);
        private final Argument<UUID> uuidArg = ArgumentType.UUID("uuid");

        public List() {
            super("list");

            setDefaultExecutor(this::listMaps);
            addSyntax(this::listMaps, pageArg);
            addSyntax(this::listMaps, uuidArg); //todo syntax should be conditional
            addSyntax(this::listMaps, uuidArg, pageArg); //todo syntax should be conditional
        }

        @Blocking
        private void listMaps(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
                return;
            }

            // Legacy maps are completely disabled in the local dev server, handle that
            if (legacy == null) {
                sender.sendMessage("Legacy maps are disabled");
                return;
            }

            int page = 0;
            if (context.has(pageArg)) page = context.get(pageArg);

            var target = player.getUuid().toString();
            if (context.has(uuidArg)) target = context.get(uuidArg).toString();

            var maps = legacy.getMapsForUuid(target)
                    .stream()
                    .skip(page * PAGE_SIZE)
                    .limit(PAGE_SIZE)
                    .toList();
            if (maps.isEmpty()) {
                if (page == 0) {
                    sender.sendMessage("You have no maps");
                } else {
                    sender.sendMessage("No more maps");
                }
                return;
            }

            sender.sendMessage("Your maps:");
            for (var map : maps) {
                sender.sendMessage(" - " + map.name() + " (" + map.id() + ")");
            }
        }
    }

    //map legacy import 0444697c-a3b1-46e8-92d3-1d58e441741e 46766
    private class Import extends Command {

        private final Argument<String> idArg = ArgumentType.String("id");
        private final Argument<UUID> uuidArg = ArgumentType.UUID("uuid");

        public Import() {
            super("import");

            setDefaultExecutor((sender, context) -> sender.sendMessage("add a map id :(")); //todo
            addSyntax(this::importMap, uuidArg, idArg);
            addSyntax(this::importMap, idArg);
        }

        @Blocking
        private void importMap(@NotNull CommandSender sender, @NotNull CommandContext context) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
                return;
            }

            // Legacy maps are completely disabled in the local dev server, handle that
            if (legacy == null) {
                sender.sendMessage("Legacy maps are disabled");
                return;
            }

            var mapId = context.get(idArg);

            var target = player.getUuid().toString();
            if (context.has(uuidArg)) target = context.get(uuidArg).toString();

            var legacyMap = legacy.getMapsForUuid(target)
                    .stream()
                    .filter(m -> m.id().equals(mapId))
                    .findFirst()
                    .orElse(null);
            if (legacyMap == null) {
                sender.sendMessage("You don't have a map with that id");
                return;
            }

            var playerData = PlayerData.fromPlayer(player);
            int slot = -1;
            for (int possibleSlot = 0; possibleSlot < PlayerData.MAX_MAP_SLOTS; possibleSlot++) {
                if (playerData.getSlotState(possibleSlot) == PlayerData.SLOT_STATE_OPEN) {
                    slot = possibleSlot;
                    break;
                }
            }
            if (slot == -1) {
                sender.sendMessage("You don't have any open map slots");
                return;
            }

            var map = legacyMap.toMapData();
            legacy.importMap(legacyMap, map.getId());
            map = server.mapStorage().createMap(map);
            map.setMapFileId(mapId);

            playerData.setMapSlot(slot, map.getId());
            server.playerStorage().updatePlayer(playerData);

            sender.sendMessage("map imported");
        }

    }

    private class Export extends Command {
        public Export() {
            super("export");

            setDefaultExecutor((sender, context) -> {
                //todo
                sender.sendMessage("Map exporting must be done through discord for now");
            });
        }
    }
}
