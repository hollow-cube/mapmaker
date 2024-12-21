package net.hollowcube.mapmaker.map.gui.effect.item;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.feature.play.effect.HotbarItems;
import net.hollowcube.mapmaker.map.item.vanilla.FireworkRocketItem;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class ItemFireworkEditor extends ItemAbstractEditor {
    private static final int MIN_AMOUNT = 0;
    private static final int MAX_AMOUNT = 99; // todo should support unlimited (with enchant glint) -> is it possible to add an ∞ overlay with rp somehow?
    private static final int MIN_DURATION = 50;
    private static final int MAX_DURATION = 86_400_000; // todo should support unlimited

    private @Outlet("amt_minus_big_switch") Switch amtMinusBigSwitch;
    private @Outlet("amt_minus_small_switch") Switch amtMinusSmallSwitch;
    private @Outlet("amt_plus_small_switch") Switch amtPlusSmallSwitch;
    private @Outlet("amt_plus_big_switch") Switch amtPlusBigSwitch;
    private @Outlet("amt_text") Text amtText;

    private @Outlet("dur_minus_big_switch") Switch durMinusBigSwitch;
    private @Outlet("dur_minus_small_switch") Switch durMinusSmallSwitch;
    private @Outlet("dur_plus_small_switch") Switch durPlusSmallSwitch;
    private @Outlet("dur_plus_big_switch") Switch durPlusBigSwitch;
    private @Outlet("dur_text") Text durText;

    public ItemFireworkEditor(@NotNull Context context, HotbarItems.@NotNull Mutable items, int index) {
        super(context, items, index);
    }

    @Override
    protected void updateFromState() {
        boolean isFirework = item != null && item.material().id() == Material.FIREWORK_ROCKET.id();

        int amount = item == null ? 1 : FireworkRocketItem.getCount(item);
        amtMinusBigSwitch.setOption(isFirework && amount > MIN_AMOUNT);
        amtMinusSmallSwitch.setOption(isFirework && amount > MIN_AMOUNT);
        amtPlusSmallSwitch.setOption(isFirework && amount < MAX_AMOUNT);
        amtPlusBigSwitch.setOption(isFirework && amount < MAX_AMOUNT);
        amtText.setText(amount <= 0 ? "Infinite" : String.valueOf(amount));

        int duration = item == null ? 0 : FireworkRocketItem.getDurationMillis(item);
        durMinusBigSwitch.setOption(isFirework && duration > MIN_DURATION);
        durMinusSmallSwitch.setOption(isFirework && duration > MIN_DURATION);
        durPlusSmallSwitch.setOption(isFirework && duration < MAX_DURATION);
        durPlusBigSwitch.setOption(isFirework && duration < MAX_DURATION);
        durText.setText(NumberUtil.formatDuration(duration));
    }

    private void addAmount(int delta) {
        if (item == null) return;
        int amount = Math.max(MIN_AMOUNT, Math.min(MAX_AMOUNT, FireworkRocketItem.getCount(item) + delta));
        updateItem(FireworkRocketItem.withCount(item, amount));
    }

    private void addDuration(int deltaMillis) {
        if (item == null) return;
        int duration = Math.max(MIN_DURATION, Math.min(MAX_DURATION, FireworkRocketItem.getDurationMillis(item) + deltaMillis));
        updateItem(FireworkRocketItem.setDurationMillis(item, duration));
    }

    @Action("amt_minus_big")
    private void amtMinusBig() {
        addAmount(-5);
    }

    @Action("amt_minus_small")
    private void amtMinusSmall() {
        addAmount(-1);
    }

    @Action("amt_plus_small")
    private void amtPlusSmall() {
        addAmount(1);
    }

    @Action("amt_plus_big")
    private void amtPlusBig() {
        addAmount(5);
    }

    @Action("dur_minus_big")
    private void durMinusBig() {
        addDuration(-5_000);
    }

    @Action("dur_minus_small")
    private void durMinusSmall() {
        addDuration(-1_000);
    }

    @Action("dur_plus_small")
    private void durPlusSmall() {
        addDuration(1_000);
    }

    @Action("dur_plus_big")
    private void durPlusBig() {
        addDuration(5_000);
    }
}
