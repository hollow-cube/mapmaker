package net.hollowcube.mapmaker.map.gui.effect.item;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.feature.play.effect.HotbarItems;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.EnchantmentList;
import net.minestom.server.item.enchant.Enchantment;
import org.jetbrains.annotations.NotNull;

public class ItemTridentEditor extends ItemAbstractEditor {
    private static final int MIN_LEVEL = 1;
    private static final int MAX_LEVEL = 3;

    private @Outlet("rl_minus_big_switch") Switch rlMinusBigSwitch;
    private @Outlet("rl_minus_small_switch") Switch rlMinusSmallSwitch;
    private @Outlet("rl_plus_small_switch") Switch rlPlusSmallSwitch;
    private @Outlet("rl_plus_big_switch") Switch rlPlusBigSwitch;
    private @Outlet("rl_text") Text rlText;

    public ItemTridentEditor(@NotNull Context context, @NotNull HotbarItems.Mutable items, int index) {
        super(context, items, index);
    }

    private int getRiptideLevel() {
        var enchants = item != null ? item.get(ItemComponent.ENCHANTMENTS, EnchantmentList.EMPTY) : EnchantmentList.EMPTY;
        return enchants.level(Enchantment.RIPTIDE);
    }

    private void setRiptideLevel(int level) {
        level = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
        var enchants = item.get(ItemComponent.ENCHANTMENTS, EnchantmentList.EMPTY);
        if (enchants.level(Enchantment.RIPTIDE) == level) return;
        enchants = enchants.with(Enchantment.RIPTIDE, level);
        updateItem(item.with(ItemComponent.ENCHANTMENTS, enchants));
    }

    @Override
    protected void updateFromState() {
        var isTrident = item != null && item.material().id() == Material.TRIDENT.id();
        var level = getRiptideLevel();

        rlMinusBigSwitch.setOption(isTrident && level > MIN_LEVEL);
        rlMinusSmallSwitch.setOption(isTrident && level > MIN_LEVEL);
        rlPlusSmallSwitch.setOption(isTrident && level < MAX_LEVEL);
        rlPlusBigSwitch.setOption(isTrident && level < MAX_LEVEL);
        rlText.setText(switch (level) {
            case 0 -> "None";
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> String.valueOf(level);
        });
        rlText.setArgs(Component.text(level));
    }

    @Action("reset")
    private void resetToDefault() {
        setRiptideLevel(1);
    }

    @Action("rl_minus_big")
    private void rlMinusBig() {
        setRiptideLevel(getRiptideLevel() - 5);
    }

    @Action("rl_minus_small")
    private void rlMinusSmall() {
        setRiptideLevel(getRiptideLevel() - 1);
    }

    @Action("rl_plus_small")
    private void rlPlusSmall() {
        setRiptideLevel(getRiptideLevel() + 1);
    }

    @Action("rl_plus_big")
    private void rlPlusBig() {
        setRiptideLevel(getRiptideLevel() + 5);
    }

    @Action("rl_text")
    private void handleSetCustomLevel() {
        pushView(c -> new TridentRiptideCustomLevelAnvil(c, String.valueOf(getRiptideLevel())));
    }

    @Signal(TridentRiptideCustomLevelAnvil.SIG_UPDATE_NAME)
    public void handleUpdateLevelFromInput(@NotNull String input) {
        if (input.isEmpty()) {
            setRiptideLevel(1);
            return;
        }

        try {
            int newLevel = Integer.parseInt(input);
            setRiptideLevel(Math.min(Math.max(newLevel, 1), 3));
        } catch (NumberFormatException ignored) {
        }
    }

}
