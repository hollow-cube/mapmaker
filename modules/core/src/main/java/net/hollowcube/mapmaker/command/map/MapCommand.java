package net.hollowcube.mapmaker.command.map;

import net.hollowcube.command.CommandBuilder;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.dsl.SimpleCommand;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.gui.map.MapListView;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.util.nats.JetStreamWrapper;

public class MapCommand extends CommandDsl {

    public final MapListCommand list;

    public final MapDeleteCommand delete;
    public final MapEditCommand edit;
    public final MapPlayCommand play;
    public final MapLeaderboardCommand leaderboard;
    public final MapAlterCommand alter;
    public final MapDrainCommand drain;

    public MapCommand(
        ApiClient api,
        PlayerService playerService,
        MapService mapService,
        ServerBridge bridge,
        JetStreamWrapper jetStream
    ) {
        super("map");

        description = "A broad command that lets you see or edit properties of a map";
        category = CommandCategories.GLOBAL;

        // Default commands
        addSubcommand(this.list = new MapListCommand(api, playerService, mapService, bridge));

        addSubcommand(SimpleCommand.of("history")
            .callback(player -> Panel.open(player, new MapListView.History(api, mapService, bridge)))
            .description("View the history of a map")
            .build()
        );

        // Permissioned commands
        addSubcommand(this.delete = new MapDeleteCommand(mapService));
        addSubcommand(this.edit = new MapEditCommand(mapService, bridge));
        addSubcommand(this.play = new MapPlayCommand(mapService, bridge));
        addSubcommand(this.leaderboard = new MapLeaderboardCommand(playerService, mapService));
        this.alter = new MapAlterCommand(mapService);
        addSubcommand(this.drain = new MapDrainCommand(mapService, jetStream));
    }

    @Override
    public void build(CommandBuilder builder) {
        super.build(builder);
        alter.build(builder);
    }
}
