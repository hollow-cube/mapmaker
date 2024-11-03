package net.hollowcube.mapmaker.command.map;

import com.google.inject.Inject;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.command.CommandBuilder;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.map.legacy.MapLegacyCommand;
import net.hollowcube.mapmaker.kafka.FriendlyProducer;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerService;
import org.jetbrains.annotations.NotNull;

public class MapCommand extends CommandDsl {

    public final MapListCommand list;
    public final MapInfoCommand info;

    public final MapDeleteCommand delete;
    public final MapEditCommand edit;
    public final MapLeaderboardCommand leaderboard;
    public final MapAlterCommand alter;
    public final MapDrainCommand drain;

    @Inject
    public MapCommand(
            @NotNull Controller guiController,
            @NotNull PlayerService playerService,
            @NotNull MapService mapService,
            @NotNull PermManager permManager,
            @NotNull ServerBridge bridge,
            @NotNull FriendlyProducer producer
    ) {
        super("map");

        description = "A broad command that lets you see or edit properties of a map";
        category = CommandCategories.GLOBAL;

        // Default commands
        addSubcommand(this.list = new MapListCommand(guiController, playerService, mapService));
        addSubcommand(this.info = new MapInfoCommand(mapService, permManager));

        addSubcommand(new MapLegacyCommand(mapService, permManager));

        // Permissioned commands
        addSubcommand(this.delete = new MapDeleteCommand(mapService, permManager));
        addSubcommand(this.edit = new MapEditCommand(mapService, permManager, bridge));
        addSubcommand(this.leaderboard = new MapLeaderboardCommand(playerService, mapService, permManager));
        this.alter = new MapAlterCommand(mapService, permManager);
        addSubcommand(this.drain = new MapDrainCommand(mapService, permManager, producer));

        // Testing
//        addSubcommand(new MapLookupCommand(mapService));
    }

    @Override
    public void build(@NotNull CommandBuilder builder) {
        super.build(builder);
        alter.build(builder);
    }
}
