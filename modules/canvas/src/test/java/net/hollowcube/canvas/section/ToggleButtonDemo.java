package net.hollowcube.canvas.section;

import net.hollowcube.canvas.section.std.ButtonSection;
import net.hollowcube.common.lang.LanguageProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.util.List;

public class ToggleButtonDemo extends ParentSection {
    private static final ItemStack PARKOUR_BUTTON = ItemStack.builder(Material.STICK)
            .meta(meta -> meta.customModelData(1000))
            .displayName(Component.translatable("gui.parkour.button"))
            .lore(LanguageProvider.optionalMultiTranslatable("gui.parkour.button", List.of()))
            .build();

    private boolean state = false;

    public ToggleButtonDemo() {
        super(9, 6+4);

        add(0, 6, new ButtonSection(3, 2, PARKOUR_BUTTON, () -> {
            state = !state;
            updateTitle();
        }));
    }

    @Override
    protected void mount() {
        super.mount();

        updateTitle();
    }

    private void updateTitle() {
        var sb = new StringBuilder();

        // Add background
        sb.append("\uF808\uF803"); // Left 11
        sb.append("\uEff8");
        sb.append("\uF80C\uF80A\uF808\uF803"); // Left 173

        if (state) {
            sb.append("\uF802"); // Left 2
            sb.append("\uEff9");
//            sb.append("\uF822"); // Right 2
        }

        find(RootSection.class).setTitle(Component.text(sb.toString(), NamedTextColor.WHITE));
    }
}
