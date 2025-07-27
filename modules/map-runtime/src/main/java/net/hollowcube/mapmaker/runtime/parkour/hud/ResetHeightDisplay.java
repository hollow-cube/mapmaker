package net.hollowcube.mapmaker.runtime.parkour.hud;

import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerUpdateStateEvent;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import net.minestom.server.tag.Tag;

import java.util.UUID;

import static net.hollowcube.mapmaker.map.util.EventUtil.playerEventNode;

public class ResetHeightDisplay {
    private static final Tag<Display> DISPLAY_TAG = Tag.Transient("mapmaker:reset_height_display");

    // TODO: the display is no longer preserved when entering spec and exiting.
    //       it should become a player setting and always be preserved.
    public static final EventNode<PlayerInstanceEvent> EVENT_NODE = playerEventNode()
            .addListener(ParkourMapPlayerUpdateStateEvent.class, ResetHeightDisplay::onPlayerStateChange);

    public static void toggle(Player player) {
        var display = player.getAndSetTag(DISPLAY_TAG, null);
        if (display != null) {
            display.hide(player);
            display.remove();
        } else {
            display = new Display(player);
            player.setTag(DISPLAY_TAG, display);
            display.show(player);
        }
    }

    public static void clear(Player player) {
        var display = player.getAndSetTag(DISPLAY_TAG, null);
        if (display != null) {
            display.hide(player);
            display.remove();
        }
    }

    private static void onPlayerStateChange(ParkourMapPlayerUpdateStateEvent event) {
        var player = event.player();
        var display = player.getTag(DISPLAY_TAG);

        if (display == null || !display.isViewer(player)) return;
        display.update(player, event.world());
    }


    @SuppressWarnings("UnstableApiUsage")
    private static class Display extends Entity {

        public Display(Player player) {
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
        public TextDisplayMeta getEntityMeta() {
            return (TextDisplayMeta) super.getEntityMeta();
        }

        @Override
        protected void movementTick() {
            // Intentionally do nothing
        }

        @Override
        public void updateOldViewer(Player player) {

        }

        public void hide(Player player) {
            if (removeViewer(player)) {
                player.sendPacket(new DestroyEntitiesPacket(this.getEntityId()));
            }
        }

        public void show(Player player) {
            this.addViewer(player);
            var world = ParkourMapWorld.forPlayer(player);
            if (world != null) update(player, world);
        }

        public void update(Player player, ParkourMapWorld world) {
            if (!(world.getPlayerState(player) instanceof ParkourState.AnyPlaying p))
                return;

            var pos = player.getPosition();
            var playState = p.saveState().state(PlayState.class);
            var resetHeight = playState.get(Attachments.RESET_HEIGHT, world.defaultResetHeight());

            var scale = Math.min(player.getSettings().viewDistance() * 16f, 256f);
            this.teleport(new Pos(pos.x(), resetHeight - 0.1f, pos.z() + scale / 2f, 0, -90));
            this.getEntityMeta().setScale(new Vec(3 * scale, 3 * scale, 1));
        }
    }
}
