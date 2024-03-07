package net.hollowcube.mapmaker.gui.store;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.CosmeticType;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.PlayerSetting;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class CosmeticView extends View {
    private static final PlayerSetting<Boolean> SHOW_LOCKED = PlayerSetting.Bool("cosmetics.show_locked", true);

    private @ContextObject PlayerService playerService;
    private @ContextObject Player player;

    private @Outlet("show_locked_switch") Switch showLockedSwitch;
    private @Outlet("cosmetic_list") Pagination pagination;

    private final PlayerDataV2 playerData;

    public CosmeticView(@NotNull Context context) {
        super(context);
        this.playerData = PlayerDataV2.fromPlayer(player);

        showLockedSwitch.setOption(playerData.getSetting(SHOW_LOCKED) ? 1 : 0);
    }

    @Action("show_locked_off")
    public void showLockedOff() {
        playerData.setSetting(SHOW_LOCKED, true);
        showLockedSwitch.setOption(1);
        pagination.reset();
    }

    @Action("show_locked_on")
    public void showLockedOn() {
        playerData.setSetting(SHOW_LOCKED, false);
        showLockedSwitch.setOption(0);
        pagination.reset();
    }

    @Action(value = "cosmetic_list", async = true)
    private void fetchPage(@NotNull Pagination.PageRequest<CosmeticEntry> request) {
        var entries = new ArrayList<CosmeticEntry>();

        var unlockedCosmetics = playerService.getUnlockedCosmetics(playerData.id());

        boolean showLocked = playerData.getSetting(SHOW_LOCKED);
        Cosmetic.values(CosmeticType.HEAD).stream()
                .sorted(Cosmetic.comparingRarity())
                .forEach(cosmetic -> {
                    var isLocked = !unlockedCosmetics.contains(cosmetic.path());
                    if (!showLocked && isLocked) return;
                    entries.add(new CosmeticEntry(request.context(), playerData, cosmetic, isLocked));
                });

        request.respond(entries, false);
    }

    @Signal(Element.SIG_CLOSE)
    private void onClose() {
        playerData.writeUpdatesUpstream(playerService);
        MiscFunctionality.applyCosmetics(player, playerData);
    }

}
