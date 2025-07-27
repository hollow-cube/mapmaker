package net.hollowcube.mapmaker.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.action.Attachments;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@AutoService(FeatureProvider.class)
public class ResetHeightDisplayFeatureProvider implements FeatureProvider {

    private static final Tag<Display> DISPLAY_TAG = Tag.Transient("mapmaker:reset_height_display");

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/reset_height/events", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::onPlayerInit)
            .addListener(PlayerMoveEvent.class, this::onPlayerMove)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::onPlayerLeave);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld || world instanceof TestingMapWorld)) return false;
        if (world.map().settings().getVariant() != MapVariant.PARKOUR) return false;

        world.eventNode().addChild(eventNode);

        return true;
    }

    private void onPlayerInit(@NotNull MapPlayerInitEvent event) {
        var world = event.mapWorld();
        var player = event.player();

        var display = player.getTag(DISPLAY_TAG);
        if (display == null) return;

        if (!world.isPlaying(player)) {
            display.hide(player);
        } else {
            display.show(player);
        }
    }

    private void onPlayerLeave(@NotNull MapWorldPlayerStopPlayingEvent event) {
        var player = event.player();
        var display = player.getTag(DISPLAY_TAG);
        if (display != null) {
            display.hide(player);
            display.remove();
            player.removeTag(DISPLAY_TAG);
        }
    }

    private void onPlayerMove(@NotNull PlayerMoveEvent event) {
        var player = event.getPlayer();
        var display = player.getTag(DISPLAY_TAG);
        var world = MapWorld.forPlayerOptional(player);

        if (display == null || !display.isViewer(player) || world == null) return;
        display.update(player, world);
    }

    public static void toggle(@NotNull Player player) {
        var display = player.getTag(DISPLAY_TAG);
        if (display != null) {
            display.hide(player);
            display.remove();
            player.removeTag(DISPLAY_TAG);
        } else {
            display = new Display(player);
            player.setTag(DISPLAY_TAG, display);
            display.show(player);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private static class Display extends Entity {

        public Display(@NotNull Player player) {
            super(EntityType.TEXT_DISPLAY, UUID.randomUUID());

            setAutoViewable(false);
            setAutoViewEntities(false);
            setNoGravity(true);
            hasPhysics = false;
            collidesWithEntities = false;
            preventBlockPlacement = false;

            var meta = getEntityMeta();

            meta.setBackgroundColor(0x40FF0000);
            meta.setText(Component.text("\u3000")); // Pretty much a square
            meta.setTextOpacity((byte) 0);
            meta.setScale(new Vec(1000, 1000, 1));
            meta.setViewRange(100f);
            meta.setPosRotInterpolationDuration(5);

            setInstance(player.getInstance(), new Pos(0, 0, 0, 0, -90));
        }

        @Override
        public @NotNull TextDisplayMeta getEntityMeta() {
            return (TextDisplayMeta) super.getEntityMeta();
        }

        @Override
        protected void movementTick() {
            // Intentionally do nothing
        }

        @Override
        public void updateOldViewer(@NotNull Player player) {

        }

        public void hide(@NotNull Player player) {
            if (removeViewer(player)) {
                player.sendPacket(new DestroyEntitiesPacket(this.getEntityId()));
            }
        }

        public void show(@NotNull Player player) {
            this.addViewer(player);
            var world = MapWorld.forPlayerOptional(player);
            if (world == null) return;
            update(player, world);
        }

        public void update(@NotNull Player player, @NotNull MapWorld world) {
            var pos = player.getPosition();

            var state = SaveState.optionalFromPlayer(player);
            if (state == null) return;
            var playState = state.tryGetState(PlayState.class);
            if (playState == null) return;

            var resetHeight = playState.get(Attachments.RESET_HEIGHT, world.instance().getTag(BaseParkourMapFeatureProvider.DEFAULT_RESET_HEIGHT));

            var scale = Math.min(player.getSettings().viewDistance() * 16f, 256f);
            this.teleport(new Pos(pos.x(), resetHeight - 0.1f, pos.z() + scale / 2f, 0, -90));
            this.getEntityMeta().setScale(new Vec(3 * scale, 3 * scale, 1));
        }
    }
}