package net.hollowcube.mapmaker.editor.gui.displayentity.search;

import com.miguelfonseca.completely.AutocompleteEngine;
import com.miguelfonseca.completely.data.Indexable;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.editor.gui.displayentity.object.DisplayEntityEntry;
import net.hollowcube.mapmaker.gui.common.anvil.AbstractSearchView;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.hollowcube.mapmaker.util.Autocompletors;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class SearchDisplaysView extends AbstractSearchView<DisplayEntityEntry> {

    public static final String SIGNAL = "search_displays.selected";

    private final AutocompleteEngine<IndexableDisplay> engine = Autocompletors.createEngine();

    public SearchDisplaysView(@NotNull Context context) {
        super(context);

        for (@NotNull Entity entity : context.player().getInstance().getEntities()) {
            if (entity instanceof DisplayEntity display) {
                this.engine.add(new IndexableDisplay(display));
            }
        }
    }

    @Override
    protected List<DisplayEntityEntry> search(@NotNull Context context, int page, int size, @NotNull String input) {
        var player = context.player();

        if (input.isEmpty()) {
            return player.getInstance().getEntities()
                    .stream()
                    .filter(entity -> entity instanceof DisplayEntity && entity.isViewer(player))
                    .sorted(Comparator.comparingDouble(entity -> entity.getDistance(player)))
                    .limit(size)
                    .map(entity -> new DisplayEntityEntry(context, (DisplayEntity) entity))
                    .toList();
        } else {
            return this.engine.search(input)
                    .stream()
                    .map(IndexableDisplay::display)
                    .filter(display -> display.isViewer(player))
                    .limit(size)
                    .map(display -> new DisplayEntityEntry(context, display))
                    .toList();
        }
    }

    @Signal(DisplayEntityEntry.SIGNAL)
    private void onSelectEntity(@NotNull UUID id) {
        popView(SIGNAL, id);
    }

    private record IndexableDisplay(DisplayEntity display) implements Indexable {

        @Override
        public List<String> getFields() {
            return switch (this.display) {
                case DisplayEntity.Item item -> {
                    var material = item.getEntityMeta().getItemStack().material();
                    yield List.of(material.name(), material.key().value(), material.key().value().replace("_", " "));
                }
                case DisplayEntity.Block block -> {
                    var material = block.getEntityMeta().getBlockStateId();
                    yield List.of(material.name(), material.key().value(), material.key().value().replace("_", " "));
                }
                case DisplayEntity.Text entity ->
                        List.of(MiniMessage.miniMessage().serialize(entity.getEntityMeta().getText()));
            };
        }
    }
}

