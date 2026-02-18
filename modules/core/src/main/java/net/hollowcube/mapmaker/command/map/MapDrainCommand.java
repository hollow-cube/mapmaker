package net.hollowcube.mapmaker.command.map;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.kafka.FriendlyProducer;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapMgmtConsumer;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.hollowcube.mapmaker.util.nats.JetStreamWrapper;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.hollowcube.command.CommandCondition.and;
import static net.hollowcube.mapmaker.command.staff.StaffCommand.IN_STAFF_MODE;

public class MapDrainCommand extends CommandDsl {
    private final Argument<@Nullable MapData> mapArg;
    private final Argument<String> reasonArg = Argument.GreedyString("reason")
        .description("The reason for draining the map");

    private final JetStreamWrapper jetStream;
    private final FriendlyProducer producer;

    public MapDrainCommand(@NotNull MapService mapService, @NotNull PermManager permManager, @NotNull JetStreamWrapper jetStream, @NotNull FriendlyProducer producer) {
        super("drain");
        this.jetStream = jetStream;
        this.producer = producer;

        description = "Drains all active instances of a map";
        examples = List.of("/map drain 123-456-789", "/map drain a12345bc-67de-8f91-ghij-2345k6l78912");

        mapArg = CoreArgument.Map("map", mapService)
            .description("The ID of the map to drain");

        setCondition(and(IN_STAFF_MODE, permManager.createPlatformCondition2(PlatformPerm.MAP_ADMIN)));
        addSyntax(playerOnly(this::handleDrainMap), mapArg);
        addSyntax(playerOnly(this::handleDrainMap), mapArg, reasonArg);
    }

    private void handleDrainMap(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        var reason = context.get(reasonArg);

        if (map == null) {
            player.sendMessage(Component.translatable(
                "command.play.map_not_found",
                Component.text(context.getRaw(mapArg))));
            return;
        }
        var message = new MapMgmtConsumer.MapUpdateMessage(MapMgmtConsumer.MapUpdateMessage.ACTION_DRAIN, map.id(), reason);
        jetStream.publish(message.subject(), message);

        // TODO: remove when no more usages exist.
        producer.produceAndForget("map_mgmt", AbstractHttpService.GSON.toJson(message));

        player.sendMessage("trying to drain map: " + map.id());
    }
}
