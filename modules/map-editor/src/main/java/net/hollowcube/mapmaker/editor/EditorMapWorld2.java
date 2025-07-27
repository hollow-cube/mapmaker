package net.hollowcube.mapmaker.editor;

import net.hollowcube.compat.axiom.AxiomPlayer;
import net.hollowcube.mapmaker.editor.feature.PickBlockFeature;
import net.hollowcube.mapmaker.map.AbstractMapWorld2;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.MapWorld2;
import net.hollowcube.mapmaker.map.util.MapWorldHelpers;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerPickBlockEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

import static net.hollowcube.mapmaker.map.util.EventUtil.playerEventNode;

public class EditorMapWorld2 extends AbstractMapWorld2 {
    private static final int WORLD_BORDER_WARNING_DISTANCE = 5;

    public static @Nullable EditorMapWorld2 forPlayer(Player player) {
        var instance = player.getInstance();
        if (instance == null) return null;

        // TODO(new worlds): we should actually get the current world for player
        //  and then check if it is an editor world, or its a test world and get
        //  parent since test is the only sub world of editor?
        // but then hypothetically what if a test world had a sub world?
        // probably need to get nearest ancestor world of the given type.
        var world = MapWorld2.forInstance(instance);
        return world instanceof EditorMapWorld2 w1 ? w1 : null;
    }

    private final Set<Player> editingPlayers = new HashSet<>();
    private final EventNode<PlayerInstanceEvent> editingEventNode = playerEventNode(editingPlayers);

    public EditorMapWorld2(MapServer server, MapData map) {
        super(server, map, makeMapInstance(map, 'e'));
        eventNode().addChild(editingEventNode);

        editingEventNode.addListener(PlayerPickBlockEvent.class, PickBlockFeature::handlePickBlock);
    }

    /// Events for only players who are actively editing the map.
    public EventNode<PlayerInstanceEvent> editingEventNode() {
        return this.editingEventNode;
    }

    @Override
    protected void configureInstance() {
        super.configureInstance();

        // Warning distance creates the red border when nearby the world border.
        instance().setWorldBorder(instance().getWorldBorder()
                .withWarningTime(WORLD_BORDER_WARNING_DISTANCE)
                .withWarningDistance(WORLD_BORDER_WARNING_DISTANCE));
    }

    @Override
    protected void loadWorld() {
        //todo
    }

    // region Lifecycle

    @Override
    public void configurePlayer(AsyncPlayerConfigurationEvent event) {
        super.configurePlayer(event);

        final var player = event.getPlayer();
        // TODO: should be reading this from save state
        player.setRespawnPoint(map().settings().getSpawnPoint());
    }

    @Override
    public void spawnPlayer(Player player) {
        super.spawnPlayer(player);
        configureEditingPlayer(player);
    }

    @NonBlocking
    private void configureEditingPlayer(Player player) {
        MapWorldHelpers.resetPlayer(player);
        player.setGameMode(GameMode.CREATIVE);

        // We don't actually know if Axiom will become present later, so just send the enable message now
        // and if they do have it installed they will get permission to use it.
        AxiomPlayer.setEnabled(player, true);

        editingPlayers.add(player);
    }

    // endregion

}
