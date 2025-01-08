package net.hollowcube.mapmaker.map.gui.effect.item;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.feature.play.effect.HotbarItem;
import net.hollowcube.mapmaker.map.feature.play.effect.HotbarItems;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.text.Component;
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

    private HotbarItem.FireworkRocket item;

    public ItemFireworkEditor(@NotNull Context context, HotbarItems.@NotNull Mutable items, int index) {
        super(context, items, index);

        if (!(items.getItem(index) instanceof HotbarItem.FireworkRocket rocket))
            throw new IllegalArgumentException("Item at index is not a firework rocket");
        item = rocket;
        updateFromState();
    }

    @Override
    protected void updateItem(@NotNull HotbarItem newItem) {
        this.item = (HotbarItem.FireworkRocket) newItem;
        super.updateItem(newItem);
    }

    @Override
    protected void updateFromState() {
        boolean isFirework = item != null;

        int amount = item == null ? 1 : item.quantity();
        amtMinusBigSwitch.setOption(isFirework && amount > MIN_AMOUNT);
        amtMinusSmallSwitch.setOption(isFirework && amount > MIN_AMOUNT);
        amtPlusSmallSwitch.setOption(isFirework && amount < MAX_AMOUNT);
        amtPlusBigSwitch.setOption(isFirework && amount < MAX_AMOUNT);
        amtText.setText(amount <= 0 ? "Infinite" : String.valueOf(amount));
        amtText.setArgs(Component.text(amount <= 0 ? "Infinite" : String.valueOf(amount)));

        int duration = item == null ? 0 : item.duration();
        durMinusBigSwitch.setOption(isFirework && duration > MIN_DURATION);
        durMinusSmallSwitch.setOption(isFirework && duration > MIN_DURATION);
        durPlusSmallSwitch.setOption(isFirework && duration < MAX_DURATION);
        durPlusBigSwitch.setOption(isFirework && duration < MAX_DURATION);
        durText.setText(String.valueOf(duration / 1000.0));
        durText.setArgs(Component.text(NumberUtil.formatDuration(duration)));
    }

    private void addAmount(int delta) {
        if (item == null) return;
        int amount = Math.max(MIN_AMOUNT, Math.min(MAX_AMOUNT, item.quantity() + delta));
        updateItem(item.withQuantity(amount));
    }

    private void addDuration(int deltaMillis) {
        if (item == null) return;
        int duration = Math.max(MIN_DURATION, Math.min(MAX_DURATION, item.duration() + deltaMillis));
        updateItem(item.withDuration(duration));
    }

    private int getAmount() {
        return item.quantity();
    }

    private int getDuration() {
        return item.duration();
    }

    private void setAmount(int newAmount) {
        if (item == null) return;
        int amount = Math.max(MIN_AMOUNT, Math.min(MAX_AMOUNT, newAmount));
        updateItem(item.withQuantity(amount));
    }

    private void setDuration(int newDurationMillis) {
        if (item == null) return;
        int duration = Math.max(MIN_DURATION, Math.min(MAX_DURATION, newDurationMillis));
        updateItem(item.withDuration(duration));
    }

    @Action("reset")
    private void resetToDefault() {
        setAmount(MIN_AMOUNT);
        setDuration(1000);
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

    @Action("amt_text")
    private void handleSetCustomAmount() {
        pushView(c -> new FireworkRocketCustomAmountAnvil(c, String.valueOf(getAmount())));
    }

    @Signal(FireworkRocketCustomAmountAnvil.SIG_UPDATE_NAME)
    public void handleUpdateAmountFromInput(@NotNull String input) {
        if (input.isEmpty()) {
            setAmount(MIN_AMOUNT);
            return;
        }

        try {
            int newAmount = Integer.parseInt(input);
            setAmount(Math.max(MIN_AMOUNT, Math.min(MAX_AMOUNT, newAmount)));
        } catch (NumberFormatException ignored) {
        }
    }

    @Action("dur_text")
    private void handleSetCustomDuration() {
        pushView(c -> new FireworkRocketCustomDurationAnvil(c, String.valueOf(getDuration() / 1000.0)));
    }

    @Signal(FireworkRocketCustomDurationAnvil.SIG_UPDATE_NAME)
    public void handleUpdateDurationFromInput(@NotNull String input) {
        if (input.isEmpty()) {
            setDuration(MIN_DURATION);
            return;
        }

        try {
            int newDuration = (int) (Double.parseDouble(input) * 1000.0);
            setDuration(Math.max(MIN_DURATION, Math.min(MAX_DURATION, newDuration)));
        } catch (NumberFormatException ignored) {
        }
    }

}
