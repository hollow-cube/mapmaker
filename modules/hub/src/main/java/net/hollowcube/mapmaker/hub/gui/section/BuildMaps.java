package net.hollowcube.mapmaker.hub.gui.section;

import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.RootSection;
import net.hollowcube.canvas.std.ButtonSection;
import net.hollowcube.canvas.std.IconSection;
import net.hollowcube.mapmaker.hub.gui.item.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class BuildMaps extends ParentSection {
    private static final ItemStack INFO_ITEM = ItemUtils.BLANK_ITEM.with(builder -> {
        builder.displayName(Component.text("Info coming soon", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
    });

    private static final ItemStack PUBLISHED_MAPS_ITEM = ItemUtils.BLANK_ITEM.with(builder -> {
        builder.displayName(Component.text("Published Maps", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        builder.lore(
                Component.text("View your previously published maps.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text(""),
                Component.text("+", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(" Click to view", NamedTextColor.GRAY)
                                .decoration(TextDecoration.BOLD, false))
        );
    });
    private static final ItemStack PLAY_WITH_FRIENDS_ITEM = ItemUtils.BLANK_ITEM.with(builder -> {
        builder.displayName(Component.text("Join A Friend", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
    });

    public BuildMaps() {
        super(9, 3);

        add(8, 0, new IconSection(INFO_ITEM));

        add(0, 2, new ButtonSection(2, 1, PUBLISHED_MAPS_ITEM, this::viewPublishedMaps));
        add(2, 2, new ButtonSection(2, 1, PLAY_WITH_FRIENDS_ITEM, this::playWithFriends));

    }

    @Override
    protected void mount() {
        super.mount();

        find(RootSection.class).setTitle(buildTitle());
    }

    private void viewPublishedMaps() {
        System.out.println("View published maps todo");
    }

    private void playWithFriends() {
        System.out.println("Play with friends todo");
    }

    private @NotNull Component buildTitle() {
        return Component.text("\uF808\uEff2", NamedTextColor.WHITE);
    }
}
