package net.hollowcube.mapmaker.hub.merchant;

import net.hollowcube.mapmaker.hub.entity.NpcPlayerEntity;
import net.minestom.server.entity.PlayerSkin;
import org.jetbrains.annotations.Nullable;

public class MerchantEntity extends NpcPlayerEntity {
    private static int nameCounter = 1;

    public MerchantEntity(@Nullable PlayerSkin skin) {
        super(String.valueOf(nameCounter++), skin);
    }
}
