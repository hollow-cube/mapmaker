package net.hollowcube.map.gui.biome;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.map2.biome.BiomeContainer;
import net.hollowcube.map2.biome.BiomeInfo;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BiomeEntry extends View {

    private @Outlet("type") Switch typeSwitch;
    private @Outlet("edit_btn") Label editButton;
    private @Outlet("edit_btn_unloaded") Label editButtonUnloaded;

    private final BiomeInfo biomeInfo;
    private final BiomeContainer container;

    public BiomeEntry(@NotNull Context context, @Nullable BiomeInfo biomeInfo, @NotNull BiomeContainer container) {
        super(context);
        this.biomeInfo = biomeInfo;
        this.container = container;

        int type = 1; // add (disabled)
        if (biomeInfo != null)
            type = biomeInfo.isLoaded() ? 2 : 3;
        else if (container.size() < container.maxSize())
            type = 0;
        typeSwitch.setOption(type);
        if (biomeInfo != null) {
            editButton.setItemSprite(ItemStack.of(biomeInfo.getDisplayItem()));
            editButtonUnloaded.setItemSprite(ItemStack.of(biomeInfo.getDisplayItem()));
        }
    }

    @Action("add_btn")
    public void handleAddBiome(@NotNull Player player) {
        var biomeInfo = container.createBiome();
        if (biomeInfo == null) return; // No more slots available

        pushView(c -> new BiomeEditorView(c, container, biomeInfo));
    }

    @Action("edit_btn")
    public void handleEditBiome() {
        if (biomeInfo == null) return; // Sanity check

        pushView(c -> new BiomeEditorView(c, container, biomeInfo));
    }


    @Action("edit_btn_unloaded")
    public void handleEditUnloadedBiome() {
        if (biomeInfo == null) return; // Sanity check

        pushView(c -> new BiomeEditorView(c, container, biomeInfo));
    }

}
