package net.hollowcube.mapmaker.editor.item;

import net.hollowcube.mapmaker.editor.entity.editor.EntityEditorDialog;
import net.hollowcube.mapmaker.editor.gui.BuilderMenuPanel;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoRegistry;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.Tool;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class BuilderMenuItem extends ItemHandler {
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hammer"));

    public static final Key ID = Key.key("mapmaker:builder_menu");
    public static final BuilderMenuItem INSTANCE = new BuilderMenuItem();

    private BuilderMenuItem() {
        super(ID, RIGHT_CLICK_ANY | LEFT_CLICK_ENTITY);
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    public void build(ItemStack.Builder builder, @Nullable CompoundBinaryTag tag) {
        super.build(builder, tag);

        // Set so the builder menu item can't break blocks,
        // useful when used on small entities so if you miss you don't remove blocks by accident.
        builder.set(DataComponents.TOOL, new Tool(List.of(), 0f, 0, false));
    }

    @Override
    protected void rightClicked(Click click) {
        var player = click.player();
        var world = MapWorld.forPlayer(player);
        if (world == null) return; // Sanity

        Panel.open(player, new BuilderMenuPanel(world.server().bridge()));
    }

    @Override
    protected void leftClicked(Click click) {
        var player = click.player();
        var world = MapWorld.forPlayer(player);
        if (world == null) return;

        if (click.entity() instanceof MapEntity<?> entity) {
            var info = MapEntityInfoRegistry.get(entity);
            if (info == null) return;

            var dialog = EntityEditorDialog.get(entity);
            if (dialog == null) return;
            player.showDialog(dialog);
        }
    }
}
