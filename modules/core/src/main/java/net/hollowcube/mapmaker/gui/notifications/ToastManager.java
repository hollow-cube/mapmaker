package net.hollowcube.mapmaker.gui.notifications;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.misc.BackgroundSpriteSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.advancements.FrameType;
import net.minestom.server.advancements.Notification;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.util.Objects;

public class ToastManager {

    private static final BackgroundSpriteSet SPRITE_SET = new BackgroundSpriteSet("generic2/toast/generic");
    private static final ItemStack EMPTY_ITEM = ItemStack.builder(Material.STICK)
            .set(DataComponents.ITEM_MODEL, "minecraft:air")
            .build();

    private static Component forceShadow(Component message) {
        var children = message.children()
            .stream()
            .map(ToastManager::forceShadow)
            .toList();
        var color = Objects.requireNonNullElse(message.color(), NamedTextColor.WHITE);
        var shadow = ShadowColor.shadowColor(
            Math.clamp((int)(color.red() * 0.25f), 0, 255),
            Math.clamp((int)(color.green() * 0.25f), 0, 255),
            Math.clamp((int)(color.blue() * 0.25f), 0, 255),
            0xFF
        );

        return message.children(children).shadowColor(shadow);
    }

    public static void showNotification(Player player, Component title, Component message) {
        var resolvedMessage = forceShadow(LanguageProviderV2.translate(message));
        var resolvedTitle = forceShadow(LanguageProviderV2.translate(title));
        var titleWidth = FontUtil.measureTextV2(resolvedTitle);
        var width = Math.max(Math.max(titleWidth, FontUtil.measureTextV2(resolvedMessage)), 140);

        var text = Component.empty()
                .append(Component.text(FontUtil.computeOffset(104 - width)))
                .append(Component.text(SPRITE_SET.build(width, true, false)).color(FontUtil.computeVerticalOffset(7)))
                .append(Component.text(FontUtil.computeOffset(10 - width)))
                .append(FontUtil.rewrite("toast_title",  resolvedTitle).decorationIfAbsent(TextDecoration.BOLD, TextDecoration.State.TRUE))
                .append(Component.text(FontUtil.computeOffset((int) (-titleWidth * 1.22f))))
                .append(resolvedMessage);

        player.sendNotification(new Notification(text, FrameType.TASK, EMPTY_ITEM));
    }
}
