package net.hollowcube.mapmaker.hub.gui.common;

import net.hollowcube.mapmaker.hub.gui.item.ItemUtils;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class InfoButton extends TranslatedButtonSection {
    public InfoButton(@NotNull String baseTranslationKey) {
        this(baseTranslationKey, true);
    }

    public InfoButton(@NotNull String baseTranslationKey, boolean useItem) {
        super(baseTranslationKey, List.of(), useItem ? ItemStack.of(Material.SEA_PICKLE) : ItemUtils.BLANK_ITEM);
    }
}
