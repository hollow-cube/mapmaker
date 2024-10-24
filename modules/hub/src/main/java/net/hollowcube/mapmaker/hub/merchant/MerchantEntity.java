package net.hollowcube.mapmaker.hub.merchant;

import net.hollowcube.mapmaker.hub.entity.BaseNpcEntity;
import net.hollowcube.mapmaker.hub.entity.NpcPlayerEntity;
import net.hollowcube.mapmaker.hub.merchant.gui.MerchantShopView;
import net.hollowcube.mapmaker.map.MapWorld;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MerchantEntity extends NpcPlayerEntity {
    private static final Logger logger = LoggerFactory.getLogger(MerchantEntity.class);

    private final MerchantData data;

    public MerchantEntity(@NotNull CompoundBinaryTag nbt) {
        super(nbt);

        var merchantId = nbt.getString("merchant_id");
        this.data = MerchantData.getById(merchantId);
        if (this.data == null) logger.warn("Unknown merchant id: {}", merchantId);
        else if (this.data.skin() != null) this.skin = this.data.skin().into();

        setHandler(this::handleInteract);
    }

    private void handleInteract(@NotNull Player player, @NotNull BaseNpcEntity npc, @NotNull PlayerHand hand, boolean isLeftClick) {
        if (data == null || hand != PlayerHand.MAIN || isLeftClick) return;

        var world = MapWorld.unsafeFromInstance(getInstance());
        if (world == null) return; // Sanity
        if (!(npc instanceof NpcPlayerEntity merchant)) return;

        world.server().guiController().show(player, c -> new MerchantShopView(c, merchant.name(), data));
    }
}
