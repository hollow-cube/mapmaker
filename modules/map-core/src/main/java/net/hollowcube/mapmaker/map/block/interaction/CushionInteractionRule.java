package net.hollowcube.mapmaker.map.block.interaction;

import net.hollowcube.mapmaker.map.entity.impl.other.CushionEntity;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.color.DyeColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public class CushionInteractionRule implements BlockInteractionRule {

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var player = interaction.player();
        var eyePosition = player.getPosition().add(0, player.getEyeHeight(), 0);
        var cursor = Objects.requireNonNullElse(interaction.cursorPosition(), Vec.ZERO);
        var position = CushionEntity.placementPosition(
            interaction, eyePosition, player.getPosition().yaw(),
            interaction.blockPosition(), interaction.blockFace(), interaction.blockPosition().add(cursor)
        );
        if (position == null || CushionEntity.isOccupied(interaction.instance(), position)) return false;

        var item = interaction.item();
        var entity = new CushionEntity(UUID.randomUUID());
        // Name first, then stored entity data, then the item's own components, so the later ones win
        var customName = item.get(DataComponents.CUSTOM_NAME);
        if (customName != null) entity.getEntityMeta().setCustomName(customName);
        var entityData = item.get(DataComponents.ENTITY_DATA);
        if (entityData != null && entityData.type() == EntityType.CUSHION && !entityData.nbt().isEmpty())
            entity.readData(entityData.nbt());
        entity.setColor(item.get(DataComponents.CUSHION_COLOR, DyeColor.WHITE));

        entity.setInstance(interaction.instance(), position);
        interaction.instance().playSound(Sound.sound(SoundEvent.ENTITY_CUSHION_PLACE, Sound.Source.BLOCK, 0.75f, 0.8f), position);
        return true;
    }

    @Override
    public @NotNull SneakState sneakState() {
        return SneakState.BOTH;
    }
}
