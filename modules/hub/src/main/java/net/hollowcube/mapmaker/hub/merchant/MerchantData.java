package net.hollowcube.mapmaker.hub.merchant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record MerchantData(
        @NotNull List<MerchantTrade> trades
) {

    public static final Codec<MerchantData> CODEC = RecordCodecBuilder.create(i -> i.group(
            MerchantTrade.CODEC.listOf().fieldOf("trades").forGetter(MerchantData::trades)
    ).apply(i, MerchantData::new));


}
