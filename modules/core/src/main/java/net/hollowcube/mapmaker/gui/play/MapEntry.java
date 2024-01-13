package net.hollowcube.mapmaker.gui.play;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.bridge.ServerBridge;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MapEntry extends View {

    private @ContextObject PlayerService playerService;
    private @ContextObject ServerBridge bridge;

    private @Outlet("btn") Label label;

    private final PersonalizedMapData map;
    private Component authorName = null;

    public MapEntry(@NotNull Context context, @NotNull PersonalizedMapData map) {
        super(context);
        this.map = map;

        label.setState(State.LOADING);
        async(this::updateIcon);
    }

    @Action("btn")
    private void handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        switch (clickType) {
            case START_SHIFT_CLICK -> bridge.joinMap(player, map.id(), HubToMapBridge.JoinMapState.PLAYING);
            case LEFT_CLICK -> pushView(c -> new MapDetailsView(c, map, authorName));
        }
    }

    /**
     * Builds and updates the arg list of the map icon.
     */
    private @Blocking void updateIcon() {
        var icon = map.settings().getIcon();
        label.setItemSprite(ItemStack.of(icon == null ? Material.PAPER : icon));

        // todo we could update the icon + title immediately and only update the lore once we have the player name perhaps
        if (authorName == null) {
            authorName = playerService.getPlayerDisplayName2(map.owner())
                    .build(DisplayName.Context.PLAIN);
        }

        var title = Component.translatable(switch (map.settings().getVariant()) {
            case PARKOUR -> "gui.play_maps.map_display.map_name.parkour";
            case BUILDING -> "gui.play_maps.map_display.map_name.building";
            case ADVENTURE -> "gui.play_maps.map_display.map_name.adventure";
        }, map.settings().getNameComponent(), map.getCompletionStateText());

        var lore = new ArrayList<Component>();
        lore.add(Component.translatable("gui.play_maps.map_display.author", authorName));
        lore.add(Component.empty());
        lore.add(Component.translatable("gui.play_maps.map_display.type", getMapTypeComponent()));
        if (map.settings().getVariant() == MapVariant.PARKOUR)
            lore.add(Component.translatable("gui.play_maps.map_display.difficulty", map.getDifficultyComponent()));
        if (map.quality() == MapQuality.GOOD) {
            lore.add(Component.translatable("gui.play_maps.map_display.rating.good"));
        } else if (map.quality() == MapQuality.GREAT) {
            lore.add(Component.translatable("gui.play_maps.map_display.rating.great"));
        } else if (map.quality() == MapQuality.EXCELLENT) {
            lore.add(Component.translatable("gui.play_maps.map_display.rating.excellent"));
        } else if (map.quality() == MapQuality.OUTSTANDING) {
            lore.add(Component.translatable("gui.play_maps.map_display.rating.outstanding"));
        } else if (map.quality() == MapQuality.MASTERPIECE) {
            lore.add(Component.translatable("gui.play_maps.map_display.rating.masterpiece"));
        } else {
            lore.add(Component.translatable("gui.play_maps.map_display.rating.unrated"));
        }

        lore.add(Component.translatable("gui.play_maps.map_display.id", Component.text(map.publishedIdString())));
        var tags = map.settings().getTags();
        if (!tags.isEmpty()) {
            lore.add(Component.empty());
            lore.add(Component.translatable("gui.play_maps.map_display.tags_header"));
            for (var tag : tags) {
                lore.add(Component.translatable("gui.play_maps.map_display.tags_single", Component.text(tag.displayName())));
            }
        }

        if (map.settings().isOnlySprint() || map.settings().isNoSprint() || map.settings().isNoJump()
                || map.settings().isNoSneak() || map.settings().isBoat()) {
            lore.add(Component.empty());
            lore.add(Component.translatable("gui.play_maps.map_display.settings_header"));

            int settingsCount = 0;
            int extraSettingsCount = 0;

            if (map.settings().isOnlySprint()) {
                lore.add(Component.translatable("gui.play_maps.map_display.settings_single", Component.text("Only Sprint")));
                settingsCount++;
            }
            if (map.settings().isNoSprint()) {
                lore.add(Component.translatable("gui.play_maps.map_display.settings_single", Component.text("No Sprint")));
                settingsCount++;
            }
            if (map.settings().isNoJump()) {
                lore.add(Component.translatable("gui.play_maps.map_display.settings_single", Component.text("No Jump")));
                settingsCount++;
            }
            if (map.settings().isNoSneak()) {
                if (settingsCount < 3) {
                    lore.add(Component.translatable("gui.play_maps.map_display.settings_single", Component.text("No Sneak")));
                } else {
                    extraSettingsCount++;
                }
                settingsCount++;
            }
            if (map.settings().isBoat()) {
                if (settingsCount < 3) {
                    lore.add(Component.translatable("gui.play_maps.map_display.settings_single", Component.text("Boats")));
                } else {
                    extraSettingsCount++;
                }
                settingsCount++;
            }

            if (settingsCount > 3) {
                lore.add(Component.translatable("gui.play_maps.map_display.setting_more", Component.text(extraSettingsCount)));
            }
        }

        lore.add(Component.empty());
        lore.addAll(LanguageProviderV2.translateMulti("gui.play_maps.map_display.footer", List.of()));

        label.setComponentsDirect(title, lore);

        label.setState(State.ACTIVE);
    }

    private @NotNull Component getMapTypeComponent() {
        if (map.settings().getVariant() == MapVariant.PARKOUR) {
            var subvariant = map.settings().getParkourSubVariant();
            return switch (Objects.requireNonNullElse(subvariant, ParkourSubVariant.SPEEDRUN)) {
                case SPEEDRUN -> Component.text("Speedrun Parkour", TextColor.color(0x15ADD3));
                case SECTIONED -> Component.text("Sectioned Parkour", TextColor.color(0x15ADD3));
                case RANKUP -> Component.text("Rankup Parkour", TextColor.color(0x15ADD3));
                case GAUNTLET -> Component.text("Gauntlet Parkour", TextColor.color(0x15ADD3));
                case DROPPER -> Component.text("Dropper Parkour", TextColor.color(0x15ADD3));
                case ONE_JUMP -> Component.text("One Jump Parkour", TextColor.color(0x15ADD3));
                case INFORMATIVE -> Component.text("Informative Parkour", TextColor.color(0x15ADD3));
            };
        } else if (map.settings().getVariant() == MapVariant.BUILDING) {
            var subvariant = map.settings().getBuildingSubVariant();
            return switch (Objects.requireNonNullElse(subvariant, BuildingSubVariant.SHOWCASE)) {
                case SHOWCASE -> Component.text("Building Showcase", TextColor.color(0x0B9F0B));
                case TUTORIAL -> Component.text("Building Tutorial", TextColor.color(0x0B9F0B));
            };
        } else {
            return Component.text("Adventure Map", TextColor.color(0x9F0B0B));
        }
    }
}
