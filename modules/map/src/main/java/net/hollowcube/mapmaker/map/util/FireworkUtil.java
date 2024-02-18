package net.hollowcube.mapmaker.map.util;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.adventure.audience.PacketGroupingAudience;
import net.minestom.server.color.Color;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.projectile.FireworkRocketMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.firework.FireworkEffect;
import net.minestom.server.item.firework.FireworkEffectType;
import net.minestom.server.item.metadata.FireworkMeta;
import net.minestom.server.network.packet.server.play.EntityStatusPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FireworkUtil {

    public static void showFirework(Player player, Instance instance, Point point, int ticks, List<FireworkEffect> effects) {
        showFirework(PacketGroupingAudience.of(List.of(player)), instance, point, ticks, effects);
    }

    public static void showFirework(PacketGroupingAudience audience, Instance instance, Point point, int ticks, List<FireworkEffect> effects) {
        var fireworkMeta = new FireworkMeta.Builder().effects(effects).build();
        var fireworkItem = ItemStack.builder(Material.FIREWORK_ROCKET).meta(fireworkMeta).build();
        var fireworkEntity = new Entity(EntityType.FIREWORK_ROCKET);
        var meta = (FireworkRocketMeta) fireworkEntity.getEntityMeta();

        var random = ThreadLocalRandom.current();

        meta.setFireworkInfo(fireworkItem);
        fireworkEntity.updateViewableRule((player) -> audience.getPlayers().contains(player));
        fireworkEntity.setNoGravity(true);
        fireworkEntity.setVelocity(new Vec(random.nextDouble(-0.01, 0.01), 1, random.nextDouble(-0.01, 0.01)));
        fireworkEntity.setInstance(instance, point);

        audience.playSound(Sound.sound(SoundEvent.ENTITY_FIREWORK_ROCKET_LAUNCH, Sound.Source.AMBIENT, 1f, 1f), point);

        fireworkEntity.scheduler().submitTask(() -> {
            if (fireworkEntity.getAliveTicks() > ticks) {
                audience.sendGroupedPacket(new EntityStatusPacket(fireworkEntity.getEntityId(), (byte) 17));
                fireworkEntity.remove();
                return TaskSchedule.stop();
            }

            // acceleration
            fireworkEntity.setVelocity(fireworkEntity.getVelocity().apply((x, y, z) -> new Vec(x * 1.15, y + 0.8, z * 1.15)));

            return TaskSchedule.nextTick();
        });
    }

    public static FireworkEffect randomColorEffect() {
        return new FireworkEffect(false, true, FireworkEffectType.STAR_SHAPED, List.of(new Color(randomRGB())), List.of(new Color(randomRGB())));
    }

    public static int randomRGB() {
        var random = ThreadLocalRandom.current();
        return java.awt.Color.HSBtoRGB(random.nextFloat(), 1f, 1f);
    }

}
