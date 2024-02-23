package net.hollowcube.mapmaker.hub.merchant;

import net.hollowcube.mapmaker.hub.entity.NpcPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

public class MerchantEntity extends NpcPlayerEntity {

    public MerchantEntity(@NotNull NBTCompound nbt) {
        super(nbt);
    }
}
