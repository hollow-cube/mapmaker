package net.hollowcube.mapmaker.hub.merchant;

import com.mojang.serialization.Codec;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record MerchantTrade(
        @NotNull List<Input> inputs,
        @NotNull Output output
) {
    public static final Codec<MerchantTrade> CODEC = Codec.unit(new MerchantTrade(List.of(), null));

    public interface Input {
        @NotNull ItemStack createItemStack(@NotNull Player player);
    }

    public interface Output {

    }

}
