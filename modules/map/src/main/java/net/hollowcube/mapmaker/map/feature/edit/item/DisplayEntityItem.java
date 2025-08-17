package net.hollowcube.mapmaker.map.feature.edit.item;

import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.map.MapFeatureFlags;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.hollowcube.mapmaker.map.gui.displayentity.AbstractEditDisplayView;
import net.hollowcube.mapmaker.map.gui.displayentity.CreateDisplayView;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DisplayEntityItem extends ItemHandler {

    public static final String ID = "mapmaker:display_entity";
    public static final DisplayEntityItem INSTANCE = new DisplayEntityItem();

    private DisplayEntityItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public void build(ItemStack.@NotNull Builder builder, @Nullable CompoundBinaryTag tag) {
        super.build(builder, tag);

        builder.material(Material.ACACIA_BOAT);
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return;
        if (!world.canEdit(player)) return;
        if (!MapFeatureFlags.DISPLAY_ENTITY_EDITOR.test(player)) return;

        var selectedId = player.getTag(DisplayEntity.SELECTED_DISPLAY_ENTITY);

        if (selectedId != null) {
            var selectedEntity = world.instance().getEntityByUuid(selectedId);
            if (selectedEntity instanceof DisplayEntity display) {
                if (player.isSneaking()) {
                    placeOnBlock(display, click);
                } else {
                    world.server().showView(player, context -> AbstractEditDisplayView.create(context, display));
                }
            } else {
                player.removeTag(DisplayEntity.SELECTED_DISPLAY_ENTITY);
            }
        } else {
            world.server().showView(player, CreateDisplayView::new);
        }
    }

    private void placeOnBlock(DisplayEntity display, Click click) {
        var direction = OpUtils.map(click.face(), BlockFace::toDirection);
        if (click.placePosition() == null || click.blockPosition() == null || direction == null) return;
        if (display instanceof DisplayEntity.Text && direction.horizontal()) {
            var pos = click.placePosition().add(Math.abs(direction.normalZ() * 0.5), 0.5, Math.abs(direction.normalX() * 0.5));
            if (direction.positive()) {
                display.teleport(Pos.fromPoint(pos));
            } else {
                display.teleport(Pos.fromPoint(pos.add(-direction.normalX(), 0, -direction.normalZ())));
            }
        } else {
            display.teleport(Pos.fromPoint(click.placePosition().add(0.5)));
        }
    }
}
