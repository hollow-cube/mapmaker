package net.hollowcube.mapmaker.map;

import net.hollowcube.command.CommandManager;
import net.hollowcube.command.util.HelpCommand;
import net.hollowcube.mapmaker.MapCommands;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.editor.command.*;
import net.hollowcube.mapmaker.editor.command.navigation.*;
import net.hollowcube.mapmaker.editor.command.utility.*;
import net.hollowcube.mapmaker.editor.hdb.HeadDatabase;
import net.hollowcube.mapmaker.editor.hdb.command.HdbCommand;
import net.hollowcube.mapmaker.editor.terraform.MapServerModule;
import net.hollowcube.mapmaker.map.block.InteractionRules;
import net.hollowcube.mapmaker.map.block.PlacementRules;
import net.hollowcube.mapmaker.map.runtime.AbstractMapServer;
import net.hollowcube.mapmaker.map.runtime.NoopServerBridge;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.misc.noop.NoopMapService;
import net.hollowcube.mapmaker.runtime.building.BuildingMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.terraform.Terraform;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

import static net.hollowcube.command.CommandCondition.or;
import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;
import static net.hollowcube.mapmaker.map.MapPlayer.simpleMapPlayer;

public class MapMapServer extends AbstractMultiMapServer {
    private static final Logger logger = LoggerFactory.getLogger(MapMapServer.class);

    private Terraform terraform;

    public MapMapServer(@NotNull ConfigLoaderV3 config) {
        super(config);
    }

    @Override
    protected @NotNull String name() {
        return "mapmaker-map";
    }

    @Override
    protected @NotNull ServerBridge createBridge() {
        return globalConfig.noop() ? new NoopServerBridge() : new MapServerBridge(this);
    }

    @Override
    protected @NotNull Future<AbstractMapWorld<?, ?>> createWorldForRequest(@NotNull MapJoinInfo joinInfo) {
        var map = mapService().getMap(joinInfo.playerId(), joinInfo.mapId());

        final boolean isEditor = Presence.MAP_BUILDING_STATES.contains(joinInfo.state());
        return createWorld(map, isEditor, _ -> {
            if (isEditor) return new EditorMapWorld(this, map, terraform);
            return switch (map.settings().getVariant()) {
                case PARKOUR -> new ParkourMapWorld(this, map);
                case BUILDING -> new BuildingMapWorld(this, map);
                default -> throw new IllegalStateException("No world for map variant " + map.settings().getVariant());
            };
        }, true);
    }

    @Override
    protected void prepareStart() {
        super.prepareStart();

        MinecraftServer.getConnectionManager()
            .setPlayerProvider(simpleMapPlayer(commandManager()));

        var terraformEvents = EventNode.event("tf-events", EventFilter.INSTANCE, event -> {
            if (event instanceof PlayerEvent pe) {
                var world = MapWorld.forPlayer(pe.getPlayer());
                return world instanceof EditorMapWorld;
            }

            return true;
        });
        var interactionEvents = EventNode.event("tf-events", EventFilter.INSTANCE, event -> {
            if (event instanceof PlayerEvent pe) {
                var world = MapWorld.forPlayer(pe.getPlayer());
                return world instanceof EditorMapWorld;
            }

            return true;
        });
        this.terraform = initBuildLogic(mapService(), commandManager(), terraformEvents, interactionEvents);
        MinecraftServer.getGlobalEventHandler().addChild(terraformEvents).addChild(interactionEvents);

        var hdb = new HeadDatabase(otel);
        addBinding(HeadDatabase.class, hdb, "headDatabase", "hdb");

        registerCommands(this, commandManager(), hdb);
    }

    // Static so it can be referenced from dev server runner
    public static @NotNull Terraform initBuildLogic(
        @NotNull MapService mapService,
        @NotNull CommandManager commandManager,
        @NotNull EventNode<InstanceEvent> terraformEvents,
        @NotNull EventNode<InstanceEvent> interactionEvents
    ) {
        // Create terraform instance
        var terraform = Terraform.builder()
            .rootEventNode(terraformEvents)
            .rootCommandManager(commandManager)
            .globalCommandCondition(builderOnly())
            .module(Terraform.BASE_MODULE)
            .module(Terraform.AXIOM_MODULE)
            .module(Terraform.WORLDEDIT_MODULE)
            .module(Terraform.VANILLA_MODULE)
            .module(MapServerModule::new)
            .storage(mapService instanceof NoopMapService ? "TerraformStorageMemory" : "TerraformStorageHttp")
            .build();

        // Block/item rules
        PlacementRules.init(terraform);
        interactionEvents.setPriority(10000000);
        InteractionRules.register(interactionEvents);

        return terraform;
    }

    // Static so it can be referenced from dev server runner
    public static void registerCommands(@NotNull AbstractMapServer server, @NotNull CommandManager commandManager, @Nullable HeadDatabase hdb) {
        // Register a second help command (regular is in registerPlayingCommands). One for terraform commands, and one for regular.
        // We test terraform commands simply by checking if they start with / (eg // commands)
        commandManager.register(new HelpCommand(
            "/help", new String[]{"/h"},
            commandManager, CommandCategories.GLOBAL,
            entry -> entry.getKey().startsWith("/")
        ));

        MapCommands.registerPlayingCommands(server, commandManager);

        commandManager.register(new TestCommand());
        commandManager.register(new BuilderMenuCommand());
        commandManager.register(new SetPreciseCoordsCommand());
        commandManager.register(new SetSpawnCommand());
        commandManager.register(new GameModeCommand());

        commandManager.register(new ClearInventoryCommand());
        commandManager.register(new GiveCommand());

        commandManager.register(new AscendCommand());
        commandManager.register(new DescendCommand());
        commandManager.register(new JumpToCommand());
        commandManager.register(new ThruCommand());
        commandManager.register(new UpCommand());
        commandManager.register(new BackCommand());

        commandManager.register(new BannerCommand());
        commandManager.register(new PHeadCommand());
        if (hdb != null) {
            commandManager.register(new HdbCommand(hdb, server.guiController()));
        }

        commandManager.register(new BiomesCommand());
//        commandManager.register(new SetBiomeCommand());

        commandManager.register(new AddMarkerCommand());
        commandManager.register(new AddInteractionCommand());
        commandManager.register(new EntitiesCommand());

        // Need to update the condition of some commands to allow during build mode.
        var fly = commandManager.xpath("fly");
        fly.setCondition(or(fly.condition(), builderOnly()));
        var flyspeed = commandManager.xpath("flyspeed");
        flyspeed.setCondition(or(flyspeed.condition(), builderOnly()));
        var teleport = commandManager.xpath("tp");
        teleport.setCondition(or(teleport.condition(), builderOnly()));

    }

}
