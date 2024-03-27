package net.hollowcube.mapmaker.gui.store;

import net.hollowcube.canvas.*;
import net.hollowcube.canvas.annotation.*;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.CosmeticType;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.PlayerSetting;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Locale;

public class CosmeticView extends View {
    private static final PlayerSetting<Boolean> SHOW_LOCKED = PlayerSetting.Bool("cosmetics.show_locked", true);

    private @ContextObject PlayerService playerService;
    private @ContextObject Player player;

    private @OutletGroup("tab_.+_switch") Switch[] tabSwitches;
    private @Outlet("tab_name_text") Text tabNameText;

    private @Outlet("show_locked_switch") Switch showLockedSwitch;
    private @Outlet("cosmetic_list") Pagination pagination;

    private final PlayerDataV2 playerData;
    private CosmeticType selectedTab = CosmeticType.HAT;

    public CosmeticView(@NotNull Context context) {
        super(context);
        this.playerData = PlayerDataV2.fromPlayer(player);

        showLockedSwitch.setOption(playerData.getSetting(SHOW_LOCKED) ? 1 : 0);

        tabSwitches[selectedTab.ordinal()].setOption(1);
        tabNameText.setText("Headwear");
        tabNameText.setArgs(Component.text("Headwear"));
        for (var cosmeticType : CosmeticType.values()) {
            var name = cosmeticType.name().toLowerCase(Locale.ROOT);
            addActionHandler("tab_" + name + "_off", Label.ActionHandler.lmb($ -> selectTab(cosmeticType)));
            addActionHandler("tab_" + name + "_on", Label.ActionHandler.lmb($ -> selectTab(cosmeticType)));
        }
    }

    public void selectTab(@NotNull CosmeticType cosmeticType) {
        if (selectedTab == cosmeticType) return;
        String displayName = switch (cosmeticType) {
            case HAT -> "Headwear";
            case BACKWEAR -> "Backwear";
            case ACCESSORY -> "Accessories";
            case PET -> "Companions";
            case EMOTE -> "Emotes";
            case PARTICLE -> "Particles";
            case VICTORY_EFFECT -> "Victory Effects";
        };

        tabNameText.setText(displayName);
        tabNameText.setArgs(Component.text(displayName));
        tabSwitches[selectedTab.ordinal()].setOption(0);
        tabSwitches[cosmeticType.ordinal()].setOption(1);
        selectedTab = cosmeticType;
        pagination.reset();
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
        Cosmetic.values(selectedTab).stream()
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
