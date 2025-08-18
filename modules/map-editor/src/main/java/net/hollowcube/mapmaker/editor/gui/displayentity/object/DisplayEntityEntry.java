package net.hollowcube.mapmaker.editor.gui.displayentity.object;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.hollowcube.mapmaker.util.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DisplayEntityEntry extends View {

    public static final String SIGNAL = "display_entity.selected";

    private @Outlet("label") Label label;

    private final @NotNull DisplayEntity entity;

    public DisplayEntityEntry(@NotNull Context context, @NotNull DisplayEntity entity) {
        super(context);

        this.entity = entity;

        var material = switch (entity) {
            case DisplayEntity.Item item -> item.getEntityMeta().getItemStack().material();
            case DisplayEntity.Block block -> block.getEntityMeta().getBlockStateId().registry().material();
            case DisplayEntity.Text ignored -> Material.PAPER;
        };

        List<Component> lore = new ArrayList<>();
        lore.add(Component.translatable("gui.display_entity.search.entry.type", Component.text(entity.getEntityType().key().toString())));
        lore.add(Component.empty());
        switch (entity) {
            case DisplayEntity.Item item -> lore.add(Component.translatable(
                    "gui.display_entity.search.entry.current",
                    LanguageProviderV2.getVanillaTranslation(item.getEntityMeta().getItemStack().material())
            ));
            case DisplayEntity.Block block -> lore.add(Component.translatable(
                    "gui.display_entity.search.entry.current",
                    LanguageProviderV2.getVanillaTranslation(block.getEntityMeta().getBlockStateId())
            ));
            case DisplayEntity.Text text -> {
                lore.add(Component.translatable("gui.display_entity.search.entry.current", Component.empty()));

                MiniMessage.miniMessage().serialize(text.getEntityMeta().getText()).lines().forEach(line ->
                        lore.add(Component.translatable(
                                "gui.display_entity.search.entry.current.entry",
                                Component.text('"').append(Component.text(line)).append(Component.text('"'))
                        ))
                );
            }
        }
        lore.add(Component.empty());
        lore.add(Component.translatable("gui.display_entity.search.entry.footer"));

        this.label.setItemSprite(ItemUtils.asDisplay(Objects.requireNonNullElse(material, Material.BARRIER)));
        this.label.setComponentsDirect(
                Component.translatable("gui.display_entity.search.entry.name", Component.text(entity.getDistance(context.player()))),
                lore
        );
    }

    @Action("label")
    private void handleSelect() {
        performSignal(SIGNAL, this.entity.getUuid());
    }


}
