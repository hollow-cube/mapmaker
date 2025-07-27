package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.OutletGroup;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapSize;
import net.hollowcube.mapmaker.map.requests.MapCreateRequest;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.store.ShopUpgrade;
import net.hollowcube.mapmaker.store.ShopUpgradeCache;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CreateMap extends View {
    public static final String SIG_MAP_CREATED = "map_created";

    private @ContextObject Player player;
    private @ContextObject MapService mapService;

    private @Outlet("submit") Label submitButton;

    private @Outlet("slot_id_create") Text slotIdText;
    private @OutletGroup("map_size_.+_switch") Switch[] sizeSwitches;

    private int slot;
    private MapSize size = MapSize.NORMAL;

    private static final int LOCKED = 2;

    public CreateMap(@NotNull Context context) {
        super(context);

        final String playerId = PlayerDataV2.fromPlayer(player).id();
        for (int i = 0; i < sizeSwitches.length; i++) {
            final MapSize size = MapSize.values()[i];
            final ShopUpgrade upgrade = switch (size) {
                case NORMAL, UNLIMITED -> null;
                case LARGE -> ShopUpgrade.MAP_SIZE_2;
                case MASSIVE -> ShopUpgrade.MAP_SIZE_3;
                case COLOSSAL -> ShopUpgrade.MAP_SIZE_4;
            };

            if (upgrade == null || ShopUpgradeCache.has(playerId, upgrade, false)) {
                addActionHandler(
                        sizeSwitches[i].id().replace("_switch", "_unset"),
                        Label.ActionHandler.lmb(player -> selectSize(size))
                );
            } else {
                sizeSwitches[i].setOption(LOCKED);
            }
        }
    }

    @Override
    public void mount() {
        super.mount();

        submitButton.setState(State.ACTIVE);
    }

    public void setSlot(int slot) {
        this.slot = slot;
        slotIdText.setText(String.format("Slot #%d", slot + 1));
        slotIdText.setArgs(Component.text(slot + 1));
        updateState();
        setState(State.ACTIVE);
    }

    private void selectSize(@NotNull MapSize size) {
        if (sizeSwitches[size.ordinal()].getOption() == LOCKED)
            return;

        this.size = size;
        updateState();
    }

    private void updateState() {
        for (int i = 0; i < sizeSwitches.length; i++) {
            if (sizeSwitches[i].getOption() == LOCKED) continue;
            sizeSwitches[i].setOption(i == size.ordinal());
        }
    }

    @Action(value = "submit", async = true)
    private void handleSubmit(@NotNull Player player) {
        submitButton.setState(State.LOADING);

        var playerData = MapPlayerData.fromPlayer(player);

        // Dispatch request to create the map
        try {
            int protocolVersion = ProtocolVersions.getProtocolVersion(player);
            var createdMap = mapService.createMap(MapCreateRequest.forPlayer(playerData.id(), size, slot, protocolVersion));
            performSignal(SIG_MAP_CREATED, slot, createdMap);
            submitButton.setState(State.ACTIVE);
        } catch (Exception e) {
            ExceptionReporter.reportException(e, player);
            player.sendMessage(Component.translatable("generic.unknown_error"));
            player.closeInventory();
        }
    }

}
