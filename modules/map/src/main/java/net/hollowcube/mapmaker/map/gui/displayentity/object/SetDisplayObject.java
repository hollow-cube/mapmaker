package net.hollowcube.mapmaker.map.gui.displayentity.object;

import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.gui.common.anvil.AbstractSearchView;
import net.hollowcube.mapmaker.util.Autocompletors;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class SetDisplayObject extends AbstractSearchView<ObjectEntry> {

    public static final String SIGNAL = "set_display_object.selected";

    private final Predicate<Material> filter;

    public SetDisplayObject(@NotNull Context context, Predicate<Material> filter) {
        super(context);

        this.filter = filter;
    }

    @Override
    protected List<ObjectEntry> search(@NotNull Context context, int page, int size, @NotNull String input) {
        if (input.isEmpty()) {
            return ThreadLocalRandom.current().ints(1, Material.values().size())
                    .mapToObj(Material::fromId)
                    .filter(Objects::nonNull)
                    .filter(this.filter)
                    .limit(size)
                    .map(m -> new ObjectEntry(context, m))
                    .toList();
        } else {
            return Autocompletors.searchMaterials(input, size, this.filter)
                    .stream()
                    .map(m -> new ObjectEntry(context, m))
                    .toList();
        }
    }

    @Signal(ObjectEntry.SIGNAL)
    private void handleSelectIcon(@NotNull Material material) {
        popView(SIGNAL, material);
    }

}
