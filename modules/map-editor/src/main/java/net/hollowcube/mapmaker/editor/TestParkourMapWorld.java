package net.hollowcube.mapmaker.editor;

import net.hollowcube.common.hud.PlayerHud;
import net.hollowcube.ipc.map.MapPatch;
import net.hollowcube.mapmaker.editor.item.ExitTestModeItem;
import net.hollowcube.mapmaker.map.LeaderboardFormatting;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.SaveStateType;
import net.hollowcube.mapmaker.map.util.spatial.Octree;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.molang.MolangExpr;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;

import static net.hollowcube.mapmaker.player.LocalPlayer.localPlayer;
import static net.kyori.adventure.text.Component.translatable;

public class TestParkourMapWorld extends ParkourMapWorld implements SubWorld {

    private final EditorMapWorld parent;

    public TestParkourMapWorld(EditorMapWorld parent) {
        super(parent.server(), parent.map(), parent.instance());
        this.parent = parent;

        itemRegistry().registerSilent(ExitTestModeItem.INSTANCE);
    }

    @Override
    public MapPatch.Builder mapPatch() { return parent == null ? super.mapPatch() : parent.mapPatch(); }

    @Override
    public ParkourState.AnyPlaying createPlayingState(SaveState saveState) {
        return new ParkourState.Testing(saveState, null);
    }

    @Override
    public Octree collisionTree() {
        return parent.collisionTree();
    }

    @Override
    public void queueCollisionTreeRebuild() {
        parent.queueCollisionTreeRebuild();
    }

    @Override
    public CompletableFuture<Void> close() {
        throw new UnsupportedOperationException("Test world may not be closed directly.");
    }

    @Override
    public final void addPlayerDirect(Player player, Runnable callback) {
        var initialState = configurePlayer(player);
        player.setTag(PLAYER_INITIAL_STATE, initialState);

        // Global scheduler end of tick is the same as a safe point tick so this should be ok.
        MinecraftServer.getSchedulerManager().scheduleEndOfTick(() -> {
            spawnPlayer(player);
            callback.run();
        });
    }

    @Override
    public ParkourState configurePlayer(Player player) {
        // Always create a dummy play state for test mode players.
        final var playerData = localPlayer(player);
        var saveState = new SaveState(UUID.randomUUID().toString(),
            map().id().toString(), playerData.id(), SaveStateType.PLAYING,
            PlayState.SERIALIZER, new PlayState());
        // Additionally set up a checkpoint at the current position as the test cp.
        var playState = saveState.state(PlayState.class);
        playState.setPos(player.getPosition());
        playState.setLastState(playState.copy());

        return new ParkourState.Testing(saveState, null);
    }

    @Override
    public void changePlayerState(Player player, ParkourState nextState, BiPredicate<Player, ParkourState> predicate) {
        if (nextState instanceof ParkourState.Playing2)
            throw new IllegalStateException("Test worlds may never be scorable");
        super.changePlayerState(player, nextState, predicate);
    }

    @Override
    public void handleTestingModeFinish(Player player, SaveState saveState) {
        var score = LeaderboardFormatting.format(map().settings().leaderboard().format(), computeScore(player, saveState));
        player.sendMessage(translatable("testing_mode.finish", score));

        if (parent.getPlayerState(player) instanceof EditorState.Testing(var testState))
            parent.changePlayerState(player, new EditorState.Building(testState));
    }

    @Override
    protected PlayerHud.@Nullable Module createTitleHud() {
        return null; // Inherit from parent
    }

    @Override
    protected void loadWorldData() {
        // Nothing for test worlds we inherit from the parent.
    }

    @Override
    protected void computeDefaultResetHeight() {
        this.defaultResetHeight = instance().getCachedDimensionType().minY() - 5;
    }

    @Override
    protected MolangExpr leaderboardScoreExpr() {
        // Always parse it inline since it can change during editing.
        return MolangExpr.parseOrThrow(map().settings().leaderboard().score());
    }
}
