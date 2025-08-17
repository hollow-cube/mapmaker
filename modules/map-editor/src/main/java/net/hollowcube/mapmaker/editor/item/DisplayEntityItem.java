package net.hollowcube.mapmaker.editor.item;

import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.editor.EditorState;
import net.hollowcube.mapmaker.editor.gui.displayentity.AbstractEditDisplayView;
import net.hollowcube.mapmaker.editor.gui.displayentity.CreateDisplayView;
import net.hollowcube.mapmaker.map.MapFeatureFlags;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;

public class DisplayEntityItem extends ItemHandler {

    public static final Key ID = Key.key("mapmaker:display_entity");
    public static final DisplayEntityItem INSTANCE = new DisplayEntityItem();

    private DisplayEntityItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public void build(ItemStack.Builder builder, @Nullable CompoundBinaryTag tag) {
        super.build(builder, tag);

        builder.material(Material.ACACIA_BOAT);
    }

    @Override
    protected void rightClicked(Click click) {
        var player = click.player();
        var world = EditorMapWorld.forPlayer(player);
        if (world == null || !(world.getPlayerState(player) instanceof EditorState.Building)) return;
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
                display.teleport(pos.asPos());
            } else {
                display.teleport(pos.asPos().add(-direction.normalX(), 0, -direction.normalZ()));
            }
        } else {
            display.teleport(click.placePosition().add(0.5).asPos());
        }
    }
}
