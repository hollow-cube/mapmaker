package net.hollowcube.mapmaker.map.gui.effect.potion;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.entity.potion.PotionEffectList;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class PotionEffectLevelEntry extends View {
    public static final String SIG_UPDATE = "potion_level.update";

    private @Outlet("root") Switch root;
    private @Outlet("select") Label selectLabel;
    private @Outlet("select_selected") Label selectSelectedLabel;
    private @Outlet("set_custom_selected") Label customSelectedLabel;

    private final PotionEffectList.Entry effect;
    private final int level;
    private final Runnable save;

    public PotionEffectLevelEntry(@NotNull Context context, @NotNull PotionEffectList.Entry effect, int level, @NotNull Runnable save) {
        super(context);
        this.effect = effect;
        this.level = level;
        this.save = save;

        if (level != -1) {
            var sprite = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("effect/potion/level/" + level), "effect/potion/level/" + level);
            var item = ItemStack.builder(Material.DIAMOND)
                    .set(DataComponents.ITEM_MODEL, Objects.requireNonNull(sprite.model(), "sprite must have a model"))
                    .build();
            selectLabel.setItemSprite(item);
            selectSelectedLabel.setItemSprite(item);
        }

        updateFromEffect();
    }

    @Action("select")
    public void handleSelectNamedLevel() {
        if (level == -1 || level > effect.type().maxLevel()) return;

        effect.setLevel(level);
        performSignal(SIG_UPDATE);
        save.run();
    }

    @Action("set_custom")
    public void handleSetCustomLevelA() {
        pushView(c -> new PotionEffectCustomLevelAnvil(c, String.valueOf(effect.level())));
    }

    @Action("set_custom_selected")
    public void handleSetCustomLevelB() {
        pushView(c -> new PotionEffectCustomLevelAnvil(c, String.valueOf(effect.level())));
    }

    @Signal(PotionEffectCustomLevelAnvil.SIG_UPDATE_NAME)
    public void updateLevelFromInput(@NotNull String input) {
        if (input.isEmpty()) return;

        try {
            int level = Integer.parseInt(input);
            if (level < 1 || level > effect.type().maxLevel()) return;
            effect.setLevel(level);
            performSignal(SIG_UPDATE);
            save.run();
        } catch (NumberFormatException ignored) {

        }
    }

    @Signal(SIG_UPDATE)
    public void updateFromEffect() {
        if (level == -1) {
            root.setOption(2 + (effect.level() > 4 ? 1 : 0));
            customSelectedLabel.setArgs(Component.text(effect.level()));
        } else {
            //noinspection PointlessArithmeticExpression
            root.setOption(0 + (effect.level() == this.level ? 1 : 0));
            selectLabel.setComponentsDirect(
                    Component.translatable("gui.effect.potion.level." + level + ".name"),
                    LanguageProviderV2.translateMulti("gui.effect.potion.level." + level + ".lore", List.of())
            );
            selectSelectedLabel.setComponentsDirect(
                    Component.translatable("gui.effect.potion.level." + level + ".selected.name"),
                    LanguageProviderV2.translateMulti("gui.effect.potion.level." + level + ".selected.lore", List.of())
            );
        }
    }

}
