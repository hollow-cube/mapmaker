package net.hollowcube.mapmaker.hub.feature.event.christmas;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.feature.event.EventData;
import net.hollowcube.mapmaker.hub.util.HubTime;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.interaction.InteractionEntity;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandler;
import net.hollowcube.mapmaker.player.PlayerData;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.MetadataDef;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class PresentObjectHandler extends ObjectEntityHandler {

    public static final String ID = "hub:present";

    private final Present model;

    private int day = -1;

    public PresentObjectHandler(InteractionEntity entity) {
        super(ID, entity);

        this.model = new Present(entity);
        this.onDataChange(null);
    }

    @Override
    public void onDataChange(@Nullable Player player) {
        this.day = this.data().getInt("day", -1);
        this.model.setModel(this.day);
    }

    @Override
    public boolean canSendToPlayer(Player player) {
        if (this.day <= 0) return false;
        return HubTime.now().getDayOfMonth() >= this.day;
    }

    @Override
    public void addViewer(MapWorld world, Player player) {
        if (!this.canSendToPlayer(player)) return;
        this.model.addViewer(player);
    }

    @Override
    public void onPlayerInteract(Player player) {
        if (!canSendToPlayer(player)) return; // Sanity check
        var world = MapWorld.forPlayer(player);
        var playerData = PlayerData.fromPlayer(player);
        var eventData = playerData.getSetting(EventData.SETTING);

        if (world == null) return;
        if (eventData.hasPresent(day)) {
            player.sendMessage("You have already collected this present!");
        } else {
            playerData.setSetting(EventData.SETTING, eventData.withPresent(day));
            FutureUtil.submitVirtual(() -> playerData.writeUpdatesUpstream(world.server().playerService()));

            player.sendMessage("You have collected your present for day " + day + "! Merry Christmas!");
        }
    }

    private static class Present extends NpcItemModel {

        private final List<SendablePacket> missingPresentPackets;

        private int day = 0;
        private long ticks = 0;

        public Present(Entity parent) {
            this.setAutoViewable(false);
            this.setInstance(parent.getInstance(), parent.getPosition().add(0, 0.5, 0).withYaw(45));

            this.missingPresentPackets = List.of(
                    createMissingPacket(Particle.EFFECT.withProperties(NamedTextColor.RED, 0.75f)),
                    createMissingPacket(Particle.EFFECT.withProperties(NamedTextColor.DARK_GREEN, 0.75f)),
                    createMissingPacket(Particle.EFFECT.withProperties(NamedTextColor.WHITE, 0.75f))
            );
        }

        public void setModel(int day) {
            this.day = day;
            this.setModel(PresentTextures.getForDay(this.day));
        }

        private boolean hasPresent(Player player) {
            var playerData = PlayerData.fromPlayer(player);
            var eventData = playerData.getSetting(EventData.SETTING);
            return eventData.hasPresent(this.day);
        }

        private SendablePacket createMissingPacket(Particle particle) {
            return new ParticlePacket(
                    particle,
                    false,
                    false,
                    position.add(0, 0.3, 0),
                    Vec.ZERO,
                    0.5f,
                    4
            );
        }

        @Override
        public void tick(long time) {
            ticks++;
            if (ticks % 20 == 0) {
                for (var player : this.viewers) {
                    if (player.getDistance(this) > 16f) continue;
                    if (hasPresent(player)) continue;
                    player.sendPackets(this.missingPresentPackets);
                }
            }
        }
    }
}
