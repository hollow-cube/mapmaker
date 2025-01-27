package net.hollowcube.mapmaker.map.gui.displayentity;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.hollowcube.mapmaker.map.feature.edit.DisplayEntityEditingFeatureProvider;
import net.hollowcube.mapmaker.map.gui.displayentity.search.SearchDisplaysView;
import net.hollowcube.terraform.event.TerraformPreSpawnEntityEvent;
import net.hollowcube.terraform.event.TerraformSpawnEntityEvent;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Consumer;

public class CreateDisplayView extends View {

    public CreateDisplayView(@NotNull Context context) {
        super(context);
    }

    @Action("item")
    private void onCreateItemDisplay(Player player) {
        spawnEntity(player, EntityType.ITEM_DISPLAY, DisplayEntity.Item.class, it -> {
            var meta = it.getEntityMeta();
            meta.setItemStack(ItemStack.of(Material.STONE));
        });
    }

    @Action("block")
    private void onCreateBlockDisplay(Player player) {
        spawnEntity(player, EntityType.BLOCK_DISPLAY, DisplayEntity.Block.class, it -> {
            var meta = it.getEntityMeta();
            meta.setBlockState(Block.STONE);
            meta.setTranslation(new Vec(-0.5, -0.5, -0.5));
        });
    }

    @Action("text")
    private void onCreateTextDisplay(Player player) {
        spawnEntity(player, EntityType.TEXT_DISPLAY, DisplayEntity.Text.class, it -> {
            var meta = it.getEntityMeta();
            meta.setText(Component.text("Hello, world!"));
            meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.VERTICAL);
            meta.setSeeThrough(true);
        });
    }

    @Action("search")
    private void onSearchDisplay(Player player) {
        pushView(SearchDisplaysView::new);
    }

    @Signal(SearchDisplaysView.SIGNAL)
    private void onSearchDisplaySelected(UUID uuid) {
        var player = this.player();
        var entity = player.getInstance().getEntityByUuid(uuid);
        if (entity instanceof DisplayEntity display) {
            DisplayEntityEditingFeatureProvider.setSelectedDisplayEntity(player, display);
            replaceView(context -> AbstractEditDisplayView.create(context, display));
        }
    }

    private <T extends DisplayEntity> void spawnEntity(Player player, EntityType type, Class<T> clazz, Consumer<T> operations) {
        Instance instance = player.getInstance();

        var preEvent = OpUtils.build(new TerraformPreSpawnEntityEvent(player, instance), EventDispatcher::call);
        if (preEvent.isCancelled()) return;

        var entity = preEvent.getConstructor().apply(type, UUID.randomUUID());
        if (entity == null) return;

        var playerPos = player.getPosition();
        var pos = new Pos(playerPos.blockX() + 0.5, playerPos.blockY() + 0.5, playerPos.blockZ() + 0.5);

        var event = OpUtils.build(new TerraformSpawnEntityEvent(player, instance, entity, pos), EventDispatcher::call);
        if (event.isCancelled()) return;

        var display = OpUtils.safeCast(event.getEntity(), clazz);
        if (display == null) return;

        display.setInstance(instance, event.getPosition());
        operations.accept(display);
        DisplayEntityEditingFeatureProvider.setSelectedDisplayEntity(player, display);
        replaceView(context -> AbstractEditDisplayView.create(context, display));
    }
}
