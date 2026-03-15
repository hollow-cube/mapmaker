package net.hollowcube.mapmaker.gui.store;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.CosmeticType;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.panels.*;
import net.hollowcube.mapmaker.panels.buttons.CycleButton;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Blocking;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.hollowcube.mapmaker.PlayerSettings.COSMETICS_SHOW_LOCKED;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.title;

public class CosmeticPanel extends Panel {

    public static final Set<CosmeticType> DISABLED_TABS = Set.of(
        CosmeticType.BACKWEAR, CosmeticType.PET, CosmeticType.EMOTE
    );
    private static final Map<CosmeticType, String> TAB_SPRITES = Map.of(
        CosmeticType.HAT, "icon2/1_1/top_hat",
        CosmeticType.BACKWEAR, "icon2/1_1/backpack",
        CosmeticType.ACCESSORY, "icon2/1_1/phone",
        CosmeticType.PET, "icon2/1_1/paw",
        CosmeticType.EMOTE, "icon2/1_1/sunglasses_face",
        CosmeticType.PARTICLE, "icon2/1_1/particles",
        CosmeticType.VICTORY_EFFECT, "icon2/1_1/trophy"
    );

    private final PlayerService players;

    private final Text title;
    private final Pagination<CosmeticType> pagination;

    private CosmeticType selected;

    public CosmeticPanel(PlayerService players) {
        this(players, CosmeticType.HAT);
    }

    public CosmeticPanel(PlayerService players, CosmeticType initial) {
        super(9, 9);
        this.players = players;
        this.selected = initial;

        add(0, 0, title("Cosmetics"));
        background("generic2/containers/extended/7x3x1", -10, -31);

        add(0, 0, backOrClose());
        this.title = add(1, 0, new Text(7, 1, "Cosmetics").background("generic2/btn/default/7_1")
            .align(Text.CENTER, Text.CENTER));

        this.pagination = add(1, 2, new Pagination<>(7, 3));
        this.pagination.fetchAsync(this::fetch);

        add(0, 6, new Text(9, 1, FontUtil.rewrite("small", "categories")).align(Text.CENTER, 6));

        var categories = add(1, 7, new RadioSelect<CosmeticType>(7, 1));
        for (var type : CosmeticType.values()) {
            categories.addOption(type, (button, selected) -> {
                button.background(selected ? "generic2/btn/selected/1_1ex" : "generic2/btn/default/1_1ex");
                button.sprite(TAB_SPRITES.get(type), 1, selected ? 3 : 1);
                button.translationKey("gui.cosmetics.tab." + type.id());

                if (DISABLED_TABS.contains(type)) {
                    button.lorePostfix(
                        List.of(Component.empty(), Component.translatable("gui.cosmetics.tab.coming_later"))
                    );
                    button.onLeftClick(() -> {}); // Disable click action
                } else if (!selected) {
                    button.lorePostfix(List.of(Component.empty(), Component.translatable("gui.cosmetics.tab.select")));
                }
            });
        }
        categories.setSelected(this.selected);
        categories.onChange(this::setCategory);
    }

    @Override
    protected void mount(InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);

        if (isInitial) {
            var data = PlayerData.fromPlayer(this.host.player());
            var filter = new CycleButton<>(1, 1, data.getSetting(COSMETICS_SHOW_LOCKED));
            filter.translationKey("gui.cosmetics.filter");
            filter.background("generic2/btn/default/1_1");
            filter.addOption(true, "gui.cosmetics.filter.locked", "icon2/1_1/lock");
            filter.addOption(false, "gui.cosmetics.filter.unlocked", "icon2/1_1/open_lock");
            filter.onChange(value -> {
                data.setSetting(PlayerSettings.COSMETICS_SHOW_LOCKED, value);
                this.pagination.reset();
            });
            add(8, 0, filter);
        }

        setCategory(this.selected);
    }

    @Override
    protected void unmount() {
        var player = this.host.player();
        var data = PlayerData.fromPlayer(player);

        super.unmount();
        FutureUtil.submitVirtual(() -> data.writeUpdatesUpstream(this.players));
        MiscFunctionality.applyCosmetics(player, data);
    }

    private void setCategory(CosmeticType type) {
        this.selected = type;
        this.pagination.reset(type);
        this.title.text(LanguageProviderV2.translateToPlain("gui.cosmetics.tab." + type.id() + ".name"));
    }

    @Blocking
    private List<? extends Element> fetch(CosmeticType type, int page, int pageSize) {
        var data = PlayerData.fromPlayer(this.host.player());
        var backpack = PlayerBackpack.fromPlayer(this.host.player());
        var unlockedCosmetics = this.players.getUnlockedCosmetics(data.id());

        boolean showLocked = data.getSetting(COSMETICS_SHOW_LOCKED);
        var cosmetics = Cosmetic.values(type)
            .stream()
            .sorted(Cosmetic.comparingName())
            .sorted(Cosmetic.comparingRarity())
            .filter(cosmetic -> (showLocked && !cosmetic.isHidden()) || unlockedCosmetics.contains(cosmetic.path()))
            .toList();

        var buttons = new ArrayList<CosmeticButton>();

        for (var cosmetic : cosmetics) {
            var locked = !unlockedCosmetics.contains(cosmetic.path());
            buttons.add(new CosmeticButton(this.players, cosmetic, locked, data, backpack, buttons));
        }
        return buttons;
    }
}
