package net.hollowcube.mapmaker.map.gui.effect.potion;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.entity.potion.PotionEffectList;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PotionEffectEntry extends View {
    public static final String SIG_REMOVE = "potion_effect/remove";

    private @Outlet("root") Switch root;
    private @Outlet("edit") Label editLabel;

    private final PotionEffectList effectList;
    private final PotionEffectList.Entry effect;
    private final Runnable save;

    public PotionEffectEntry(@NotNull Context context, @Nullable PotionEffectList effectList, @Nullable PotionEffectList.Entry effect, @NotNull Runnable save) {
        super(context);
        this.effectList = effectList;
        this.effect = effect;
        this.save = save;

        root.setOption(effect == null ? 1 : 0);
        if (effect != null) {
            editLabel.setItemDirect(effect.type().icon().with(builder -> {
                builder.lore(LanguageProviderV2.translateMulti("gui.effect.potion.edit.lore", List.of(
                        Component.text(effect.level()), effect.durationComponent())));
            }));
        }
    }

    @Action("edit")
    public void handleEdit(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        if (effect == null) return;
        if (clickType == ClickType.LEFT_CLICK) {
            pushView(c -> new PotionEffectEditorView(c, effect, save));
        } else if (clickType == ClickType.RIGHT_CLICK) {
            performSignal(SIG_REMOVE, effect.type());
            save.run();
        }
    }

    @Action("add")
    public void handleAdd(@NotNull Player player) {
        pushTransientView(c -> new PotionEffectSelectorView(c, effectList, save));
    }

}
