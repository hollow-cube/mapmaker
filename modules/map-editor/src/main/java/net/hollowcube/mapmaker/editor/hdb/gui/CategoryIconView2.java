package net.hollowcube.mapmaker.editor.hdb.gui;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.editor.hdb.HeadDatabase;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CategoryIconView2 extends View {
    public static final String SIG_SELECTED = "category_icon_view.selected";

    private @Outlet("root") Switch rootSwitch;
    private @Outlet("label_off") Label labelOff;
    private @Outlet("label_on") Label labelOn;

    private final String category;

    public CategoryIconView2(@NotNull Context context, @NotNull HeadDatabase hdb, @NotNull String category) {
        super(context);
        this.category = category;

        var categoryIcon = hdb.categoryIcon(category);

        rootSwitch.setOption(0);
        var args = List.<Component>of(Component.text(0)); //todo
        var name = Component.translatable("hdb.category." + category + ".name");
        var loreOff = LanguageProviderV2.translateMulti("hdb.category." + category + ".unselected.lore", args);
        labelOff.setItemSprite(categoryIcon);
        labelOff.setComponentsDirect(name, loreOff);
        var loreOn = LanguageProviderV2.translateMulti("hdb.category." + category + ".selected.lore", args);
        labelOn.setItemSprite(categoryIcon);
        labelOn.setComponentsDirect(name, loreOn);
    }

    @Signal(SIG_SELECTED)
    public void handleSelectionChange(@NotNull String newCategory) {
        rootSwitch.setOption(this.category.equals(newCategory));
    }

    @Action("label_off")
    private void handleSelect() {
        // Do nothing if already selected
        if (rootSwitch.getOption() == 1) return;

        performSignal(SIG_SELECTED, category);
    }
}
