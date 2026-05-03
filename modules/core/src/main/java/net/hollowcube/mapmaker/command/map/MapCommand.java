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
import org.jetbrains.annotations.NotNull;

public class MapCommand extends CommandDsl {

    public final MapListCommand list;

    public final MapDeleteCommand delete;
    public final MapEditCommand edit;
    public final MapPlayCommand play;
    public final MapLeaderboardCommand leaderboard;
    public final MapAlterCommand alter;
    public final MapDrainCommand drain;

    public MapCommand(
        @NotNull ApiClient api,
        @NotNull PlayerService playerService,
        @NotNull MapService mapService,
        @NotNull ServerBridge bridge,
        @NotNull JetStreamWrapper jetStream
    ) {
        super("map");

        description = "A broad command that lets you see or edit properties of a map";
        category = CommandCategories.GLOBAL;

        // Default commands
        addSubcommand(this.list = new MapListCommand(api, mapService, bridge));

        addSubcommand(SimpleCommand.of("history")
            .callback(player -> Panel.open(player, new MapListView.History(api, mapService, bridge)))
            .description("View the history of a map")
            .build()
        );

        // Permissioned commands
        addSubcommand(this.delete = new MapDeleteCommand(api.maps));
        addSubcommand(this.edit = new MapEditCommand(api.maps, bridge));
        addSubcommand(this.play = new MapPlayCommand(api.maps, bridge));
        addSubcommand(this.leaderboard = new MapLeaderboardCommand(api, mapService));
        this.alter = new MapAlterCommand(api.maps);
        addSubcommand(this.drain = new MapDrainCommand(api.maps, jetStream));
    }

    @Override
    public void build(@NotNull CommandBuilder builder) {
        super.build(builder);
        alter.build(builder);
    }
}
