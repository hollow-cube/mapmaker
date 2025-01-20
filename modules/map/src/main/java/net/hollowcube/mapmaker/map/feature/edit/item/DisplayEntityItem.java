package net.hollowcube.mapmaker.map.feature.edit.item;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.hollowcube.mapmaker.map.gui.displayentity.AbstractEditDisplayView;
import net.hollowcube.mapmaker.map.gui.displayentity.CreateDisplayView;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class DisplayEntityItem extends ItemHandler {

    public static final String ID = "mapmaker:display_entity";
    public static final DisplayEntityItem INSTANCE = new DisplayEntityItem();

    private DisplayEntityItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @NotNull Material material() {
        return Material.ACACIA_BOAT;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return;
        if (!world.canEdit(player)) return;

        var selectedId = player.getTag(DisplayEntity.SELECTED_DISPLAY_ENTITY);

        if (selectedId != null) {
            var selectedEntity = world.instance().getEntityByUuid(selectedId);
            if (selectedEntity != null) {
                if (player.isSneaking() && click.placePosition() != null) {
                    selectedEntity.teleport(Pos.fromPoint(click.placePosition().add(0.5)));
                } else if (selectedEntity instanceof DisplayEntity display) {
                    world.server().showView(player, context -> AbstractEditDisplayView.create(context, display));
                }
            } else {
                player.removeTag(DisplayEntity.SELECTED_DISPLAY_ENTITY);
            }
        } else {
            world.server().showView(player, CreateDisplayView::new);
        }
    }

}
