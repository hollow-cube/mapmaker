package net.hollowcube.mapmaker.editor.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static net.hollowcube.mapmaker.editor.command.EditorConditions.editorWorld;

public class RemoveCommand extends CommandDsl {

    private final Argument<EntityFinder> targetArg = Argument.Entity("player").onlyPlayers(true);

    private final ServerBridge bridge;

    public RemoveCommand(ServerBridge bridge) {
        super("remove");
        this.bridge = bridge;

        setCondition(editorWorld());
        addSyntax(playerOnly(this::handleRemoveTarget), targetArg);
    }

    private void handleRemoveTarget(Player player, CommandContext context) {
        var target = context.get(targetArg).findFirstPlayer(player);
        String playerName = context.getRaw(targetArg);
        if (target == null) {
            player.sendMessage(Component.translatable("generic.player.offline", Component.text(playerName)));
            return;
        }
        if (player.equals(target)) {
            player.sendMessage(Component.translatable("generic.other_players_only"));
            return;
        }
        if (playerName.length() > 16 || playerName.length() < 3) {
            player.sendMessage(Component.translatable("generic.player_name_length"));
            return;
        }

        var senderMap = MapWorld.forPlayer(player); // Always present due to command condition
        if (!doesPlayerOwnMap(player, senderMap)) {
            player.sendMessage(Component.translatable("map.build.cant_remove"));
            return;
        }
        var targetMap = MapWorld.forPlayer(target);
        if (!senderMap.equals(targetMap)) {
            player.sendMessage(Component.translatable("map.build.remove.same_map"));
            return;
        }

        // All preconditions OK, actually remove the player from the map.
        try {
            bridge.joinHub(target);
            target.sendMessage(Component.translatable("map.build.removed", Component.text(player.getUsername())));
            player.sendMessage(Component.translatable("map.build.remove", Component.text(target.getUsername())));

        } catch (Exception e) {
            throw new RuntimeException("failed to remove player from map", e);
        }
    }

    private static boolean doesPlayerOwnMap(Player player, @Nullable MapWorld world) {
        return world != null && player.getUuid().equals(UUID.fromString(world.map().owner()));
    }
}
