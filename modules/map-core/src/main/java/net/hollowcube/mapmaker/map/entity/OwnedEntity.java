package net.hollowcube.mapmaker.map.entity;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public interface OwnedEntity {

    Key ownedEntityType();

    CompoundBinaryTag saveOwnedEntityData();

}
