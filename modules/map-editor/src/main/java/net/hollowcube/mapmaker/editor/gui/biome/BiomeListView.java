package net.hollowcube.mapmaker.editor.gui.biome;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.biome.BiomeContainer;

import java.util.ArrayList;

public class BiomeListView extends View {
    private @Outlet("gui_title") Text guiTitleText; // todo just write me on the texture

    private @Outlet("biome_list") Pagination pagination;
    private @Outlet("capacity") Text capacityText;

    private final BiomeContainer container;

    public BiomeListView(Context context, BiomeContainer biomes) {
        super(context);
        this.container = biomes;

        guiTitleText.setText("Custom Biomes");
    }

    @Signal(Element.SIG_MOUNT)
    public void redraw() {
        String capacity = String.format("%d/%d Biome Slots Used", container.size(), container.maxSize());
        capacityText.setText(capacity);
        // capacityText.setArgs(Component.translatable("gui.biome_list.capacity.name", Component.text(capacity)));
        // TODO make this work
    }

    @Action(value = "biome_list")
    public void fetchPage(Pagination.PageRequest<BiomeEntry> request) {
        var entries = new ArrayList<BiomeEntry>();

        for (var biome : container.values()) {
            entries.add(new BiomeEntry(request.context(), biome, container));
        }

        // If there is still remaining space, add the + button
        if (entries.size() < request.pageSize()) {
            entries.add(new BiomeEntry(request.context(), null, container));
        }

        request.respond(entries, false);
    }

}
