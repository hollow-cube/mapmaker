package net.hollowcube.mapmaker.cosmetic.impl.victory;

import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.adventure.audience.PacketGroupingAudience;
import net.minestom.server.color.Color;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.projectile.FireworkRocketMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.FireworkExplosion;
import net.minestom.server.item.component.FireworkList;
import net.minestom.server.network.packet.server.play.EntityStatusPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FireworkVictoryEffectImpl extends AbstractVictoryEffectImpl {
    public FireworkVictoryEffectImpl(@NotNull Cosmetic cosmetic) {
        super(cosmetic);
    }

    @Override
    public void trigger(@NotNull Player player, @NotNull Point position) {
        var viewersAndSelf = new ArrayList<Player>(player.getViewers());
        viewersAndSelf.add(player);
        player.scheduleNextTick(ignored -> showFirework(
                PacketGroupingAudience.of(viewersAndSelf),
                player.getInstance(),
                position,
                15,
                List.of(randomColorEffect())
        ));
    }

    public static void showFirework(PacketGroupingAudience audience, Instance instance, Point point, int ticks, List<FireworkExplosion> effects) {
        var fireworks = new FireworkList((byte) 0, effects);
        var fireworkItem = ItemStack.builder(Material.FIREWORK_ROCKET)
                .set(DataComponents.FIREWORKS, fireworks)
                .build();
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

    public static FireworkExplosion randomColorEffect() {
        return new FireworkExplosion(FireworkExplosion.Shape.STAR, List.of(new Color(randomRGB())), List.of(new Color(randomRGB())), true, false);
    }

    public static int randomRGB() {
        var random = ThreadLocalRandom.current();
        return java.awt.Color.HSBtoRGB(random.nextFloat(), 1f, 1f);
    }
}
