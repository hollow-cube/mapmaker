package net.hollowcube.mapmaker.hub.merchant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.mapmaker.map.util.DynamicRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record MerchantData(
        @NotNull List<MerchantTrade> trades
) {

    public static final Codec<MerchantData> CODEC = RecordCodecBuilder.create(i -> i.group(
            MerchantTrade.CODEC.listOf().fieldOf("trades").forGetter(MerchantData::trades)
    ).apply(i, MerchantData::new));

    private static final Map<String, MerchantData> REGISTRY = DynamicRegistry.get("hub_merchants", MerchantData.CODEC);


    public static @Nullable MerchantData getById(@Nullable String id) {
        if (id == null) return null;
        return REGISTRY.get(id);
    }
}
