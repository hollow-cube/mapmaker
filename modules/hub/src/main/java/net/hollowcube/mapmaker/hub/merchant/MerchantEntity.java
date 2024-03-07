package net.hollowcube.mapmaker.hub.merchant;

import net.hollowcube.mapmaker.hub.entity.BaseNpcEntity;
import net.hollowcube.mapmaker.hub.entity.NpcPlayerEntity;
import net.hollowcube.mapmaker.hub.merchant.gui.MerchantShopView;
import net.hollowcube.mapmaker.map.MapWorld;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MerchantEntity extends NpcPlayerEntity {
    private static final Logger logger = LoggerFactory.getLogger(MerchantEntity.class);

    private final MerchantData data;

    public MerchantEntity(@NotNull NBTCompound nbt) {
        super(nbt);

        var merchantId = nbt.getString("merchant_id");
        this.data = MerchantData.getById(merchantId);
        if (this.data == null) logger.warn("Unknown merchant id: {}", merchantId);

        setHandler(this::handleInteract);
    }

    private void handleInteract(@NotNull Player player, @NotNull BaseNpcEntity npc, @NotNull Player.Hand hand) {
        if (data == null || hand != Player.Hand.MAIN) return;

        var world = MapWorld.unsafeFromInstance(getInstance());
        if (world == null) return; // Sanity

        world.server().guiController().show(player, c -> new MerchantShopView(c, data));
    }
}
