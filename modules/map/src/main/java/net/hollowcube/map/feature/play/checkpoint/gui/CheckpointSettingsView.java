package net.hollowcube.map.feature.play.checkpoint.gui;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.map.feature.play.checkpoint.CheckpointPlateBlock;
import net.hollowcube.map.feature.play.checkpoint.CheckpointSetting;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.network.packet.server.play.EffectPacket;
import net.minestom.server.utils.MathUtils;
import org.jetbrains.annotations.NotNull;

public class CheckpointSettingsView extends View {

    private @Outlet("reset_height") Label resetHeightLabel;

    private final Instance instance;
    private final Point blockPosition;
    private Block block; // Updated to reflect changes

    private boolean changed = false;

    public CheckpointSettingsView(
            @NotNull Context context,
            @NotNull Instance instance,
            @NotNull Point blockPosition,
            @NotNull Block block
    ) {
        super(context);
        this.instance = instance;
        this.blockPosition = blockPosition;
        this.block = block;

        resetHeightLabel.setArgs(block.getTag(CheckpointSetting.RESET_HEIGHT_TEXT));
    }

    @Action("pick_up")
    private void pickUpCheckpoint(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        var item = CheckpointPlateBlock.ITEM.buildItemStack(block.nbt());
        player.getInventory().addItemStack(item);

        if (clickType == ClickType.START_SHIFT_CLICK) {
            // Delete the block on close, which happens next
            block = Block.AIR;
            changed = true;
        }

        player.closeInventory();
    }

    @Action("reset_height")
    private void modifyResetHeight(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        int resetHeight = block.getTag(CheckpointSetting.RESET_HEIGHT);
        int newResetHeight = resetHeight;
        if (newResetHeight == -1) newResetHeight = blockPosition.blockY() - 1; // Set to default

        switch (clickType) {
            case LEFT_CLICK -> newResetHeight++;
            case RIGHT_CLICK -> newResetHeight--;
            default -> {
                return;
            }
        }

        // Clamp to valid range, and exit early if no change was actually made
        newResetHeight = MathUtils.clamp(newResetHeight,
                instance.getDimensionType().getMinY() - 1,
                blockPosition.blockY() - 1);
        if (resetHeight == newResetHeight) return;

        changed = true;
        block = block.withTag(CheckpointSetting.RESET_HEIGHT, newResetHeight);
        resetHeightLabel.setArgs(block.getTag(CheckpointSetting.RESET_HEIGHT_TEXT));
    }

    @Signal(SIG_CLOSE)
    private void onClose() {
        if (!changed) return;
        instance.setBlock(blockPosition, block);

        // If the block was removed, play a block break effect
        if (block.isAir()) {
            var packet = new EffectPacket(2001, blockPosition, CheckpointPlateBlock.VANILLA_BLOCK.stateId(), false);
            instance.sendGroupedPacket(packet);
        }
    }

}
