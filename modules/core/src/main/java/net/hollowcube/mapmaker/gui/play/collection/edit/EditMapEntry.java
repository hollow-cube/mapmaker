package net.hollowcube.mapmaker.gui.play.collection.edit;

import net.hollowcube.canvas.ClickType;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.gui.play.ProgressMapEntry;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.PersonalizedMapData;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.util.ItemUtils;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class EditMapEntry extends View implements ProgressMapEntry {

    public static final String SIGNAL_SELECT = "collection.edit.selected";
    public static final String SIGNAL_REMOVE = "collection.edit.remove";

    private @ContextObject PlayerService playerService;
    private @ContextObject ServerBridge bridge;

    private @Outlet("btn") Label label;

    private final MapData map;

    private PersonalizedMapData.Progress progress = null; // null is unknown
    private int playtime = 0;
    private DisplayName authorName = null;

    public EditMapEntry(@NotNull Context context, @NotNull MapData map) {
        super(context);
        this.map = map;

        label.setState(State.LOADING);
        async(this::updateIcon);
    }

    @Override
    public @NotNull MapData map() {
        return map;
    }

    @Override
    public void setProgress(PersonalizedMapData.Progress progress, int playtime) {
        this.progress = progress;
        this.playtime = playtime;
        async(this::updateIcon);
    }

    @Action("btn")
    protected void handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        switch (clickType) {
            case SHIFT_LEFT_CLICK -> performSignal(SIGNAL_REMOVE, this.map);
            case LEFT_CLICK -> performSignal(SIGNAL_SELECT, this.map);
        }
    }

    private @Blocking void updateIcon() {
        var icon = map.settings().getIcon();
        if (icon == null) {
            label.setItemSprite(ItemStack.of(Material.PAPER));
        } else {
            label.setItemSprite(ItemUtils.asDisplay(icon));
        }

        if (authorName == null) {
            try {
                authorName = playerService.getPlayerDisplayName2(map.owner());
            } catch (Exception e) {
                ExceptionReporter.reportException(e, player());
                authorName = new DisplayName(List.of(new DisplayName.Part("username", "!error!", null)));
            }
        }

        var entry = MapData.createHoverComponents(map, authorName.build(),
                progress == null ? null : Map.entry(progress, playtime));
        entry.getValue().addAll(LanguageProviderV2.translateMulti("gui.edit_map_collection.map_display.footer", List.of()));
        label.setComponentsDirect(entry.getKey(), entry.getValue());

        label.setState(State.ACTIVE);
    }
}