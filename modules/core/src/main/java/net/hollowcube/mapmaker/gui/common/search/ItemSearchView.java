package net.hollowcube.mapmaker.gui.common.search;

import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.util.Autocompletors;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ItemSearchView extends AbstractSearchView<ItemSearchEntry> {

    private final Predicate<Material> filter;
    private final Consumer<Material> callback;

    public ItemSearchView(@NotNull Context context, Predicate<Material> filter, Consumer<Material> callback) {
        super(context);

        this.filter = filter;
        this.callback = callback;
    }

    @Override
    protected List<ItemSearchEntry> search(@NotNull Context context, int page, int size, @NotNull String input) {
        if (input.isEmpty()) {
            return ThreadLocalRandom.current().ints(1, Material.values().size())
                    .mapToObj(Material::fromId)
                    .filter(Objects::nonNull)
                    .filter(this.filter)
                    .limit(size)
                    .map(m -> new ItemSearchEntry(context, m))
                    .toList();
        } else {
            return Autocompletors.searchMaterials(input, size, this.filter)
                    .stream()
                    .map(m -> new ItemSearchEntry(context, m))
                    .toList();
        }
    }

    @Signal(ItemSearchEntry.SIGNAL)
    private void handleSelectIcon(@NotNull Material material) {
        this.callback.accept(material);
        popView();
    }

}