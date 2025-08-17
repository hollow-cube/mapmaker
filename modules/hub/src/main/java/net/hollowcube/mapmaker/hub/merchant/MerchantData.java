package net.hollowcube.mapmaker.hub.merchant;

import net.hollowcube.mapmaker.map.util.DynamicRegistry;
import net.hollowcube.mapmaker.player.PlayerSkin;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record MerchantData(
        @Nullable PlayerSkin skin,
        @NotNull List<MerchantTrade> trades
) {

    public static final StructCodec<MerchantData> CODEC = StructCodec.struct(
            "skin", PlayerSkin.CODEC.optional(), MerchantData::skin,
            "trades", MerchantTrade.CODEC.list(), MerchantData::trades,
            MerchantData::new);

    private static final Map<String, MerchantData> REGISTRY = DynamicRegistry.get("hub_merchants", MerchantData.CODEC);

    public MerchantData(@NotNull Optional<PlayerSkin> skin, @NotNull List<MerchantTrade> trades) {
        this(skin.orElse(null), trades);
    }

    public @NotNull Optional<PlayerSkin> optSkin() {
        return Optional.ofNullable(skin);
    }

    public static @Nullable MerchantData getById(@Nullable String id) {
        if (id == null) return null;
        return REGISTRY.get(id);
    }
}
