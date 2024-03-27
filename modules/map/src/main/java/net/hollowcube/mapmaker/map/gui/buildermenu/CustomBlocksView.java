package net.hollowcube.mapmaker.map.gui.buildermenu;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.block.custom.CheckpointPlateBlock;
import net.hollowcube.mapmaker.map.block.custom.FinishPlateBlock;
import net.hollowcube.mapmaker.map.block.custom.StatusPlateBlock;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CustomBlocksView extends View {

    public CustomBlocksView(@NotNull Context context) {
        super(context);
    }

//    @Action("give_finish_flag")
//    private void giveFinishFlag(@NotNull Player player) {
//
//    }

    @Action("give_finish_plate")
    private void giveFinishPlate(@NotNull Player player) {
        BuilderMenuView.giveCustomItem(player, FinishPlateBlock.ITEM);
    }

    @Action("give_checkpoint_plate")
    private void giveCheckpointPlate(@NotNull Player player) {
        BuilderMenuView.giveCustomItem(player, CheckpointPlateBlock.ITEM);
    }

    @Action("give_status_plate")
    private void giveStatusPlate(@NotNull Player player) {
        BuilderMenuView.giveCustomItem(player, StatusPlateBlock.ITEM);
    }
}
