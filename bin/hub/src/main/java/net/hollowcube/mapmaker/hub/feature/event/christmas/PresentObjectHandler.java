package net.hollowcube.mapmaker.hub.feature.event.christmas;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.feature.event.EventData;
import net.hollowcube.mapmaker.hub.util.HubTime;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.interaction.InteractionEntity;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandler;
import net.hollowcube.mapmaker.player.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PresentObjectHandler extends ObjectEntityHandler {

    public static final String ID = "hub:present";
    private static final Tag<Thread> CLICK_TASK = Tag.Transient("hub/event/christmas/present/click_task");

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
        var now = HubTime.now();
        if (now.getMonthValue() != 12) return false;
        return now.getDayOfMonth() >= this.day;
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
        if (world == null) return;

        var playerData = PlayerData.fromPlayer(player);
        var eventData = playerData.getSetting(EventData.SETTING);
        var service = world.server().playerService();
        var playerId = playerData.id();

        if (eventData.hasPresent(day)) {
            player.sendMessage(Component.translatable("advent.present.already_found"));
        } else {
            var reward = PresentConstants.getRewardForDay(eventData.getPresentCount() + 1);
            playerData.setSetting(EventData.SETTING, eventData.withPresent(day));

            player.updateTag(CLICK_TASK, previous -> {
                if (previous != null && previous.isAlive()) return previous;
                return FutureUtil.createVirtual(() -> {
                    playerData.writeUpdatesUpstream(service);
                    if (reward != null && !service.getUnlockedCosmetics(playerId).contains(reward.path())) {
                        service.buyCosmetic(playerId, reward, 0, 0, null);
                        player.sendMessage(Component.translatable("advent.present.found_cosmetic", reward.displayName()));
                    } else {
                        player.sendMessage(Component.translatable("advent.present.found"));
                    }
                });
            });
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
            this.setModel(PresentConstants.getTextureForDay(this.day));
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
