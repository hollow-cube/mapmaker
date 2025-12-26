package net.hollowcube.mapmaker.hub.feature.event.newyears;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.projectile.FireworkRocketMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.FireworkList;
import net.minestom.server.network.packet.server.play.EntityStatusPacket;
import net.minestom.server.sound.SoundEvent;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Firework extends Entity {

    private static final FireworkList EMPTY_EXPLOSIONS = new FireworkList(0, List.of());

    private final int ticks;

    public Firework(int ticks) {
        this(ticks, EMPTY_EXPLOSIONS);
    }

    public Firework(int ticks, FireworkList explosions) {
        super(EntityType.FIREWORK_ROCKET);
        this.ticks = ticks;

        var random = ThreadLocalRandom.current();

        this.getEntityMeta().setFireworkInfo(
            ItemStack.builder(Material.FIREWORK_ROCKET).set(DataComponents.FIREWORKS, explosions).build()
        );
        this.setNoGravity(true);
        this.setVelocity(new Vec(random.nextDouble(-0.01, 0.01), 1, random.nextDouble(-0.01, 0.01)));
    }

    @Override
    public FireworkRocketMeta getEntityMeta() {
        return (FireworkRocketMeta) super.getEntityMeta();
    }

    @Override
    public void spawn() {
        this.instance.playSound(Sound.sound(SoundEvent.ENTITY_FIREWORK_ROCKET_LAUNCH, Sound.Source.AMBIENT, 1f, 1f), this.position);
    }

    @Override
    public void update(long time) {
        if (this.getAliveTicks() > this.ticks) {
            this.explode();
        } else {
            this.setVelocity(this.getVelocity().apply((x, y, z) -> new Vec(x * 1.15, y + 0.8, z * 1.15)));
        }
    }

    protected void explode() {
        this.sendPacketToViewers(new EntityStatusPacket(this.getEntityId(), (byte) 17));
        this.remove();
    }
}
