package net.hollowcube.terraform.tool;

import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Consumer;

public interface BuiltinTool {
    @NotNull Tag<String> TYPE = Tag.String("terraform:tool/type");

    @NotNull String TYPE_CUSTOM = "terraform:custom";

    int RIGHT_CLICK_AIR = 1 << 1;
    int RIGHT_CLICK_BLOCK = 1 << 2;
    int RIGHT_CLICK_ENTITY = 1 << 3;
    int LEFT_CLICK_AIR = 1 << 4;
    int LEFT_CLICK_BLOCK = 1 << 5;
    int LEFT_CLICK_ENTITY = 1 << 6;

    @NotNull Key key();

    default @NotNull String name() {
        return key().asString();
    }

    int flags();

    @NotNull Material material();


    void leftClicked(@NotNull Click click);

    void rightClicked(@NotNull Click click);


    record Click(
            @NotNull BuiltinTool tool,
            @NotNull Player player,
            @NotNull ItemStack itemStack,
            @NotNull PlayerHand hand,
            @UnknownNullability Point blockPosition,
            @UnknownNullability Point placePosition,
            @UnknownNullability BlockFace face,
            @UnknownNullability Entity entity
    ) {
        public @UnknownNullability Instance instance() {
            return player.getInstance();
        }

        public void updateItemStack(@NotNull Consumer<ItemStack.Builder> func) {
            player.setItemInHand(hand, itemStack.with(func));
        }
    }

}
