package net.hollowcube.mapmaker.hub.feature.misc;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.cosmetic.Hats;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.entity.BaseNpcEntity;
import net.hollowcube.mapmaker.hub.entity.NpcPlayer;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.hub.feature.contest.MapContest;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@AutoService(HubFeature.class)
public class BirthdayGuyFeature implements HubFeature {
    private static final Pos BIRTHDAY_NPC_POS = new Pos(-3.5, 40, 4.5, -135, 0);
    private static final Tag<Thread> CLICK_TASK = Tag.Transient("birthday_guy_click_task");

    private MapServer server;

    @Override
    public void load(@NotNull MapServer server, @NotNull HubMapWorld world) {
        this.server = server;

        var now = LocalDateTime.now(ZoneId.of("America/New_York"));
        long millisToSpawn = ChronoUnit.MILLIS.between(now, MapContest.START_DATE) - (60 * 1000);
        if (millisToSpawn <= 0) {
            spawnNpc(world, false);
        } else {
            server.scheduler().buildTask(() -> spawnNpc(world, true))
                    .delay(TaskSchedule.millis(millisToSpawn))
                    .schedule();
        }
    }

    private void handlePlaytimeClick(@NotNull Player player, @NotNull BaseNpcEntity npc, @NotNull PlayerHand hand, boolean isLeftClick) {
        if (hand != PlayerHand.MAIN || isLeftClick || !player.getItemInMainHand().isAir()) return;

        player.updateTag(CLICK_TASK, old -> {
            if (old != null && old.isAlive()) return old;
            return FutureUtil.createVirtual(() -> {
                var playerId = PlayerDataV2.fromPlayer(player).id();
                var unlockedCosmetics = server.playerService().getUnlockedCosmetics(playerId);
                if (unlockedCosmetics.contains(Hats.CAKE_HAT.path())) {
                    player.sendMessage(Component.translatable("birthday_guy.cake.1_year.have"));
                    return;
                }

                server.playerService().buyCosmetic(playerId, Hats.CAKE_HAT, 0, 0, null);
                player.sendMessage(Component.translatable("birthday_guy.cake.1_year.give"));
                FutureUtil.sleep(1000);
            });
        });
    }

    private void spawnNpc(@NotNull HubMapWorld world, boolean withEffects) {
        if (withEffects) {
            world.instance().playSound(Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE, Sound.Source.PLAYER, 1, 1), BIRTHDAY_NPC_POS);
            world.instance().sendGroupedPacket(new ParticlePacket(Particle.EXPLOSION_EMITTER, BIRTHDAY_NPC_POS, Vec.ZERO, 0, 0));
            world.instance().sendGroupedPacket(new ParticlePacket(Particle.EXPLOSION_EMITTER, BIRTHDAY_NPC_POS, Vec.ZERO, 0, 0));
            world.instance().sendGroupedPacket(new ParticlePacket(Particle.EXPLOSION_EMITTER, BIRTHDAY_NPC_POS, Vec.ZERO, 0, 0));
        }

        var skin = new PlayerSkin(
                "ewogICJ0aW1lc3RhbXAiIDogMTc1MDQ2Nzg1NjY5OSwKICAicHJvZmlsZUlkIiA6ICJhMzYzNDQyODQwYTA0NWIzODU4M2EzYjU4MTNkNjRjNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJTZXRoUFJHIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzhjNzZmZTEyYTYwMGIyODk1YWQxNDVjYTRiM2E4NWU4MGNjZmUzOTVhOGZmOWJhNTRjYTk3ODNmZjQxMzUyNGEiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==",
                "nmsueLV+SxbzKORsFmIf5+f4pygZ+xDd+oVddgkRV5sM/vJILSFAP3sJHY7Gjg/KrxY7o+Zym95GroWjgOy4NKXgYhVYIc6SzlWft0jilcrEjD2jUJi5hVcbaEFlqjN7pBWvxugKZZ6YHNzToSDTZLIMEA3MpPwDxXuI5qfSbBCiHKgDC6YXquqnz+aOgB6pMAkpjuR3LZTKcglC95zs41DqenKP8DQXSlx1ayAzLVAESPDPp6wvFY08JTVBS1utwVJS4YcaMpewWy4/semxf/zTizQ3CmFt7/QrlWsr2p5g4Ibi7F/d9vcJUQWjWg75hS4Jof04LNTPDUkQ5ukzA/pkBi8MVZXaJk8owADgWb2M7chS0+ucMU5sgqjh4P9UcXDFx7A6WXe0Y9czsLqy1VveeHb+3dByw5zcun6gTASUsevoomoOq+wAt9HC+USOzIZBJrbJEojHv0jAiHtFmpsrRMAusKHfPk1RjZ3hElDSVedVE/U38UQfuba30LZTVJAevaxaD4Ea7bFEnEuLUgfRWKW0mzCQ3OLvtX9oOfFxX2IGW0tVS/XmDy0bxkptU4MDtZV5aRwpLDVgcIO7oWB1YebqIz6IWmx3LZrONv+Xl7zxBTM28b6XtDNzRGCaQagpkBPTd0jeVN5ym7/tLX8Zu4p7tvWUvaaLiChXKCo="
        );
        var playtimeNpc = new NpcPlayer("Birthday Guy", skin);
        playtimeNpc.setEquipment(EquipmentSlot.HELMET, Hats.CAKE_HAT.impl().iconItem());
        playtimeNpc.setEquipment(EquipmentSlot.MAIN_HAND, Hats.CAKE_HAT.impl().iconItem());
        playtimeNpc.setEquipment(EquipmentSlot.OFF_HAND, Hats.CAKE_HAT.impl().iconItem());
        playtimeNpc.setInstance(world.instance(), BIRTHDAY_NPC_POS);
        playtimeNpc.setHandler(this::handlePlaytimeClick);
    }

}
