package net.hollowcube.mapmaker.editor;

import net.hollowcube.compat.axiom.AxiomPlayer;
import net.hollowcube.mapmaker.editor.feature.PickBlockFeature;
import net.hollowcube.mapmaker.map.AbstractMapWorld2;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.util.MapWorldHelpers;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerPickBlockEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import org.jetbrains.annotations.NonBlocking;

import java.util.HashSet;
import java.util.Set;

import static net.hollowcube.mapmaker.map.util.EventUtil.playerEventNode;

public class EditorMapWorld2 extends AbstractMapWorld2 {

    private final Set<Player> editingPlayers = new HashSet<>();
    private final EventNode<PlayerInstanceEvent> editingEventNode = playerEventNode(editingPlayers);

    public EditorMapWorld2(MapServer server, MapData map) {
        super(server, map, makeMapInstance(map, 'e'));
        instance().eventNode().addChild(editingEventNode);

        editingEventNode.addListener(PlayerPickBlockEvent.class, PickBlockFeature::handlePickBlock);
    }

    /// Events for only players who are actively editing the map.
    public EventNode<PlayerInstanceEvent> editingEventNode() {
        return this.editingEventNode;
    }

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
}
