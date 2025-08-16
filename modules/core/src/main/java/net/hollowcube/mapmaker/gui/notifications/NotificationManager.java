package net.hollowcube.mapmaker.gui.notifications;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.misc.BackgroundSpriteSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.advancements.FrameType;
import net.minestom.server.advancements.Notification;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

// Can't use components because we use vertical offset color to move it upwards so components would only take the other decorations and not the color.
// So instead we just use strings directly to not give the impression that the color is part of the text.
public class NotificationManager {

    private static final BackgroundSpriteSet SPRITE_SET = new BackgroundSpriteSet("generic2/toast/generic");
    private static final ItemStack EMPTY_ITEM = ItemStack.builder(Material.STICK)
            .set(DataComponents.ITEM_MODEL, "minecraft:air")
            .build();

    public static void showNotification(@NotNull Player player, @NotNull String title, @NotNull String message) {
        showNotification(player, title, message, NamedTextColor.GRAY);
    }

    public static void showNotification(
            @NotNull Player player, @NotNull String title, @NotNull String message, @NotNull TextColor messageColor
    ) {
        var titleWidth = FontUtil.measureTextV2(title);
        var width = Math.max(titleWidth, FontUtil.measureTextV2(message));

        var text = Component.empty()
                .append(Component.text(FontUtil.computeOffset(104 - width)))
                .append(Component.text(SPRITE_SET.build(width, true, false)).color(FontUtil.computeVerticalOffset(7)))
                .append(Component.text(FontUtil.computeOffset(10 - width)))
                .append(Component.text(title, FontUtil.computeVerticalOffset(-10)).decorate(TextDecoration.BOLD))
                .append(Component.text(FontUtil.computeOffset((int) (-titleWidth * 1.18f))))
                .append(Component.text(message, messageColor).shadowColor(ShadowColor.shadowColor(TextColor.lerp(0.66f, messageColor, NamedTextColor.BLACK), 0xFF)));

        player.sendNotification(new Notification(text, FrameType.TASK, EMPTY_ITEM));
    }
}
