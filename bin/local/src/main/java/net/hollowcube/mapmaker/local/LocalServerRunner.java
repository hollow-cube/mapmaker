package net.hollowcube.mapmaker.local;

import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.local.svc.LocalMapService;
import net.hollowcube.mapmaker.local.svc.LocalPlayerService;
import net.hollowcube.mapmaker.local.svc.LocalSessionService;
import net.hollowcube.mapmaker.map.MapServerRunner;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.command.DebugCommand;
import net.hollowcube.mapmaker.map.feature.play.effect.HotbarItems;
import net.hollowcube.mapmaker.map.gui.effect.item.ItemEditorView;
import net.hollowcube.mapmaker.map.item.vanilla.FireworkRocketItem;
import net.hollowcube.mapmaker.map.util.MapJoinInfo;
import net.hollowcube.mapmaker.obungus.ObungusCore;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.SessionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class LocalServerRunner extends MapServerRunner {
    public static final String DUMMY_MAP_ID = "0557bb41-225a-4556-af2e-51f72c0a005c"; // PigianaJones

    private final Path workspace = Path.of("/Users/matt/Downloads/localmapmakertest");

    private MapService mapService = new LocalMapService(workspace);
    private PlayerService playerService = new LocalPlayerService();
    private SessionService sessionService = new LocalSessionService();

    LocalServerRunner(@NotNull ConfigLoaderV3 config) {
        super(config);
    }

    @Override
    protected @NotNull String name() {
        return "local-mapmaker";
    }

    @Override
    public @NotNull MapService mapService() {
        return mapService;
    }

    @Override
    public @NotNull PlayerService playerService() {
        return playerService;
    }

    @Override
    public @NotNull SessionService sessionService() {
        return sessionService;
    }

    @Override
    protected @NotNull CompletableFuture<@Nullable MapJoinInfo> getPendingJoin(@NotNull String playerId, boolean deleteCompleted) {
        return CompletableFuture.completedFuture(new MapJoinInfo(playerId, ObungusCore.REVIEW_MAP_ID, "editing")); // DUMMY_MAP_ID
    }

    @Override
    protected @NotNull DebugCommand createDebugCommand() {
        var cmd = super.createDebugCommand();
        cmd.createLocalSubcommand("item", (sender, context) -> {
            var items = new HotbarItems.Mutable(HotbarItems.EMPTY, null);
            items.setItem(0, FireworkRocketItem.DEFAULT_ITEM);
            guiController().show(sender, c -> new ItemEditorView(c, items));
        }, "item editor");
        return cmd;
    }
}
