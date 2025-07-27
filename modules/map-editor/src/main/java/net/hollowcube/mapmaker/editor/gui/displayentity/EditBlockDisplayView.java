package net.hollowcube.mapmaker.editor.gui.displayentity;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.BlockUtil;
import net.hollowcube.mapmaker.editor.gui.displayentity.object.PropertyEntry;
import net.hollowcube.mapmaker.editor.gui.displayentity.search.SearchMaterialsView;
import net.hollowcube.mapmaker.map.block.BlockTags;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class EditBlockDisplayView extends AbstractEditDisplayView<DisplayEntity.Block, BlockDisplayMeta> {

    private static final Predicate<Material> FILTER = material -> material.isBlock() &&
            !material.block().isAir() &&
            !BlockTags.UNRENDERABLE_DISPLAY_ENTITY_BLOCKS.contains(material.block().key());
    private static final Set<String> IGNORED_PROPERTIES = Set.of("waterlogged");

    private @Outlet("block") Label block;
    private @Outlet("properties") Pagination properties;

    public EditBlockDisplayView(@NotNull Context context, DisplayEntity.Block display) {
        super(context, display);

        this.updateState();
    }

    @Override
    protected void updateState() {
        super.updateState();
        this.block.setArgs(LanguageProviderV2.getVanillaTranslation(this.meta().getBlockStateId()));
    }

    // Display

    @Action("block")
    private void openBlockChooser() {
        this.pushView(context -> new SearchMaterialsView(context, FILTER));
    }

    @Action("properties")
    private void fillPropertiesList(@NotNull Pagination.PageRequest<PropertyEntry> request) {
        Block block = this.meta().getBlockStateId();
        Block defaultBlock = block.defaultState();

        List<PropertyEntry> result = new ArrayList<>();
        for (var entry : BlockUtil.getBlockProperties(block).entrySet()) {
            if (IGNORED_PROPERTIES.contains(entry.getKey())) continue;
            result.add(new PropertyEntry(
                    request.context(),
                    entry.getKey(),
                    entry.getValue(),
                    defaultBlock.getProperty(entry.getKey()),
                    block.getProperty(entry.getKey())
            ));
        }
        request.respond(result, false);
    }

    @Signal(PropertyEntry.SIGNAL)
    private void handleSelectIcon(@NotNull String property, @NotNull String value) {
        this.meta().setBlockState(this.meta().getBlockStateId().withProperty(property, value));
    }

    @Signal(SearchMaterialsView.SIGNAL)
    private void handleSetDisplayItem(Material material) {
        this.meta().setBlockState(material.block());
        this.updateState();
        this.properties.reset();
    }

}
