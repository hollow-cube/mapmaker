package net.hollowcube.mapmaker.gui.play.collection.edit;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.util.CursorRequest;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.gui.common.anvil.TextInputView;
import net.hollowcube.mapmaker.gui.common.search.ItemSearchView;
import net.hollowcube.mapmaker.gui.play.collection.BaseMapCollectionView;
import net.hollowcube.mapmaker.map.MapCollection;
import net.hollowcube.mapmaker.map.MapData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;

public class EditMapCollectionView extends BaseMapCollectionView<EditMapEntry> {

    private static final Predicate<@Nullable Material> SEARCH_PREDICATE = material ->
            material != null &&
                    material != Material.AIR &&
                    material != Material.SCULK_SENSOR &&
                    material != Material.CALIBRATED_SCULK_SENSOR &&
                    material != Material.RECOVERY_COMPASS &&
                    !material.name().endsWith("glass_pane");

    protected @Outlet("name") Text name;
    protected @Outlet("icon") Label icon;

    private MapData selectedMap;

    public EditMapCollectionView(@NotNull Context context, @NotNull String id) {
        super(context, id);

        this.addActionHandler("name", Label.ActionHandler.lmb(player ->
                this.pushTransientView(c -> TextInputView.builder()
                        .title("Edit Collection Name")
                        .callback(this::setName)
                        .build(c, this.collection.name())
                )
        ));

        this.addActionHandler("icon", Label.ActionHandler.lmb(player ->
                this.pushTransientView(c -> new ItemSearchView(c, SEARCH_PREDICATE, this::setIcon))
        ));
    }

    @Override
    protected void onLoaded(@NotNull MapCollection collection) {
        this.name.setText(Objects.requireNonNullElse(collection.name(), "Unnamed Collection"));
        this.name.setArgs(Objects.requireNonNullElse(this.ownerName, Component.text("Unknown").color(NamedTextColor.RED)));
        this.icon.setArgs(OpUtils.mapOr(collection.icon(), LanguageProviderV2::getVanillaTranslation, Component.text("None").color(NamedTextColor.RED)));
    }

    @Override
    protected EditMapEntry createEntry(@NotNull Context context, @NotNull MapData data) {
        return new EditMapEntry(context, data);
    }

    private void setName(@NotNull String name) {
        this.collection = this.collection.withName(name);
        this.mapService.updateMapCollection(this.player.getUuid().toString(), this.collection);
        this.onLoaded(this.collection);
    }

    private void setIcon(@NotNull Material material) {
        this.collection = this.collection.withIcon(material);
        this.mapService.updateMapCollection(this.player.getUuid().toString(), this.collection);
        this.onLoaded(this.collection);
    }

    @Signal(EditMapEntry.SIGNAL_SELECT)
    private void selectMap(@NotNull MapData map) {
        if (this.selectedMap == null) {
            this.selectedMap = map;
            this.setDirty();
        } else if (map.equals(this.selectedMap)) {
            this.selectedMap = null;
            this.setDirty();
        } else {
            var maps = this.collection.mapIds();
            var oldIndex = maps.indexOf(this.selectedMap.id());
            var newIndex = maps.indexOf(map.id());
            if (oldIndex != -1 && newIndex != -1) {
                maps.remove(oldIndex);
                maps.add(newIndex, this.selectedMap.id());
            }
            this.mapService.updateMapCollection(this.player.getUuid().toString(), this.collection);
            this.selectedMap = null;

            this.pagination.reset();
        }
    }

    @Signal(Element.SIG_GET_CURSOR)
    private void getCursor(@NotNull CursorRequest request) {
        if (this.selectedMap == null) return;
        request.respond(OpUtils.mapOr(this.selectedMap.settings().getIcon(), ItemStack::of, ItemStack.AIR));
    }
}
