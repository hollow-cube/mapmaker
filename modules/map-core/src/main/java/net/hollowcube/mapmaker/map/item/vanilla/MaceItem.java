package net.hollowcube.mapmaker.map.item.vanilla;

import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.entity.impl.projectile.WindChargeEntity;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.entity.EntityType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.enchant.Enchantment;
import net.minestom.server.item.enchant.LevelBasedValue;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class MaceItem extends VanillaItemHandler {
    private static final float EXPLOSION_RADIUS = 3.5f;
    private static final LevelBasedValue KNOCKBACK_MULTIPLIER = new LevelBasedValue.Lookup(
            List.of(1.2f, 1.75f, 2.2f), new LevelBasedValue.Linear(1.5f, 0.35f));
    private static final Set<EntityType> ALLOWED_SMASHABLE_ENTITIES = Set.of(EntityType.SLIME, EntityType.INTERACTION);

    public static final MaceItem INSTANCE = new MaceItem();

    public static @NotNull ItemStack get(int windBurst) {
        var stack = INSTANCE.getItemStack(1);
        stack = stack.with(DataComponents.ENCHANTMENTS,
                        EnchantmentList.EMPTY.with(Enchantment.WIND_BURST, windBurst))
                // Remove attribute modifiers to remove cooldown. Will eventually add
                // a cooldown seconds option to the checkpoint item.
                .without(DataComponents.ATTRIBUTE_MODIFIERS);
        return stack;
    }

    private MaceItem() {
        super(Material.MACE, false, LEFT_CLICK_ENTITY);
    }

    @Override
    protected void leftClicked(@NotNull Click click) {
        var entity = click.entity();
        if (entity == null || !ALLOWED_SMASHABLE_ENTITIES.contains(entity.getEntityType()))
            return;

        if (!(click.player() instanceof MapPlayer player))
            return;

        // This is an inlined version of the explode enchantment effect. At some point
        // we should just implement enchantment effects properly so this isnt necessary.

        if (player.isFlying() || player.getPose() == EntityPose.FALL_FLYING || player.fallDistance() < 1.5)
            return; // Cannot do explosion

        player.setVelocity(player.getVelocity().withY(0.01));
//        player.trackImpulsePosition(, true); todo

        int windBurstLevel = click.itemStack().get(DataComponents.ENCHANTMENTS, EnchantmentList.EMPTY)
                .level(Enchantment.WIND_BURST);
        if (windBurstLevel < 1) return; // Sanity

        // We dont track entity on ground, so just assume we are always smashing on the ground
        SoundEvent sound = player.fallDistance() > 5.0 ? SoundEvent.ITEM_MACE_SMASH_GROUND_HEAVY : SoundEvent.ITEM_MACE_SMASH_GROUND;
        player.playSound(Sound.sound(sound, Sound.Source.PLAYER, 1, 1), player.getPosition());

        float kbMult = KNOCKBACK_MULTIPLIER.calc(windBurstLevel);
        WindChargeEntity.sendExplosion(List.of(click.player()), player.getPosition(), EXPLOSION_RADIUS,
                kbMult, SoundEvent.ENTITY_WIND_CHARGE_WIND_BURST, Particle.GUST_EMITTER_SMALL,
                Particle.GUST_EMITTER_LARGE, false);
    }
}
