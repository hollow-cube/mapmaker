package net.hollowcube.mapmaker.map.gui.displayentity.object;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.hollowcube.mapmaker.util.TagUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class DisplayEntityEntry extends View {

    public static final String SIGNAL = "display_entity.selected";

    private @Outlet("label") Label label;

    private final @Nullable DisplayEntity entity;

    public DisplayEntityEntry(@NotNull Context context, @Nullable DisplayEntity entity) {
        super(context);

        this.entity = entity;

        if (this.entity != null) {
            var material = switch (entity) {
                case DisplayEntity.Item item -> item.getEntityMeta().getItemStack().material();
                case DisplayEntity.Block block ->
                        Objects.requireNonNullElse(block.getEntityMeta().getBlockStateId().registry().material(), Material.BARRIER);
                case DisplayEntity.Text ignored -> Material.PAPER;
                default -> Material.BARRIER;
            };
            var iconItem = ItemStack.builder(material);
            TagUtil.removeTooltipExtras(iconItem);
            this.label.setItemSprite(iconItem.build());
            this.label.setArgs(
                    Component.text(entity.getDistance(context.player())),
                    Component.text(entity.getEntityType().namespace().toString()),
                    switch (entity) {
                        case DisplayEntity.Item item -> LanguageProviderV2.getVanillaTranslation(item.getEntityMeta().getItemStack().material());
                        case DisplayEntity.Block block -> LanguageProviderV2.getVanillaTranslation(block.getEntityMeta().getBlockStateId());
                        case DisplayEntity.Text text -> Component.text('"').append(text.getEntityMeta().getText()).append(Component.text('"'));
                        default -> Component.empty();
                    }
            );
        } else {
            this.label.setComponentsDirect(Component.translatable("gui.display_entity.no_results"), List.of());
        }
    }

    @Action("label")
    private void handleSelect() {
        if (this.entity == null) return;
        performSignal(SIGNAL, this.entity.getUuid());
    }


}
