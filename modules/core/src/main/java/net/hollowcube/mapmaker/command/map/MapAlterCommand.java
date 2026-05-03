package net.hollowcube.mapmaker.command.map;

import com.google.gson.JsonElement;
import net.hollowcube.command.CommandBuilder;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.setting.MapSetting;
import net.hollowcube.mapmaker.player.Permission;
import net.kyori.adventure.text.Component;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import static net.hollowcube.command.dsl.CommandDsl.playerOnly;
import static net.hollowcube.mapmaker.command.CoreCommandCondition.staffPerm;

/**
 * Notably not using {@link net.hollowcube.command.dsl.CommandDsl}, it doesn't support arguments followed by "subcommands" very well.
 */
@SuppressWarnings("UnstableApiUsage")
public class MapAlterCommand {

    private final Argument<@Nullable MapData> mapArg;
    private final Argument<MapVariant> typeArg = Argument.Enum("type", MapVariant.class)
        .description("The new type for the map");
    private final Argument<String> nameArg = Argument.GreedyString("name")
        .description("The new name for the map");
    private final Argument<Material> displayItemArg = Argument.Material("item")
        .description("The new display item for the map");
    private final Argument<String> subvariantArg;
    private final Argument<MapSize> sizeArg = Argument.Enum("size", MapSize.class)
        .description("The new world border size for the map");
    private final Argument<MapTags.Tag> tagArg = Argument.Enum("tag", MapTags.Tag.class)
        .description("The tag to modify");
    private final Argument<MapQuality> qualityArg = Argument.Enum("quality", MapQuality.class)
        .description("The new quality rating for the map");
    private final Argument<MapSetting<?>> settingsArg = CoreArgument.MapSetting("setting")
        .description("The setting to modify");
    private final Argument<JsonElement> settingDataArg = CoreArgument.Json("data")
        .description("The new data for the setting");
    private final Argument<Boolean> listedArg = Argument.Bool("listed")
        .description("Whether the map should be listed");
    private final Argument<String> versionArg = Argument.Word("version")
        .with(ProtocolVersions.SUPPORTED_PROTOCOL_NAMES)
        .description("The new minimum required version for the map");

    private final MapClient maps;

    public MapAlterCommand(@NotNull MapClient maps) {
        this.maps = maps;

        mapArg = CoreArgument.Map("map", maps)
            .description("The ID of the map to edit");

        var subvariantTypes = new ArrayList<String>();
        for (var bsv : BuildingSubVariant.values())
            subvariantTypes.add(bsv.name().toLowerCase());
        for (var psv : ParkourSubVariant.values())
            subvariantTypes.add(psv.name().toLowerCase());
        subvariantArg = Argument.Word("variant").with(subvariantTypes)
            .description("The new variant for the map");
    }

    public void build(@NotNull CommandBuilder builder) {
        builder.child("alter", root -> root
            .condition(staffPerm(Permission.GENERIC_STAFF))
            .description("Edit information related to a map")
            .child(mapArg, alter -> alter
                .child("type", di -> di
                    .executes(playerOnly(this::handleSetType), typeArg)
                    .description("Change the type of a map")
                    .examples("/map alter 123-456-789 type parkour"))
                .child("name", name -> name
                    .executes(playerOnly(this::handleSetName), nameArg)
                    .description("Change the name of a map")
                    .examples("/map alter 123-456-789 name Floating Parkour"))
                .child("displayItem", di -> di
                    .executes(playerOnly(this::handleSetDisplayItem), displayItemArg)
                    .description("Change the display item of a map")
                    .examples("/map alter 123-456-789 displayItem book"))
                .child("variant", sv -> sv
                    .executes(playerOnly(this::handleSetSubVariant), subvariantArg)
                    .description("Change the variant of a map")
                    .examples("/map alter 123-456-789 variant speedrun"))
                .child("size", size -> size
                    .executes(playerOnly(this::handleSetSize), sizeArg)
                    .description("Change the world size of a map")
                    .examples("/map alter 123-456-789 size large"))
                .child("tag", tag -> tag
                    .description("Add or remove a tag from a map")
                    .examples("/map alter 123-456-789 tag add puzzle", "/map alter 123-456-789 tag remove story")
                    .child("add", add -> add
                        .executes(playerOnly(this::handleAddTag), tagArg)
                        .description("Add a tag to a map"))
                    .child("remove", rem -> rem
                        .executes(playerOnly(this::handleRemoveTag), tagArg)
                        .description("Remove a tag from a map")))
                .child("quality", quality -> quality
                    .executes(playerOnly(this::handleSetQuality), qualityArg)
                    .description("Change the rating of a map")
                    .examples("/map alter 123-456-789 quality unrated"))
                .child("setting", setting -> setting
                    .description("Change a setting of a map")
                    .executes(playerOnly(this::handleSetSetting), settingsArg, settingDataArg)
                    .examples("/map alter 123-456-789 setting reset_in_water false"))
                .child("listed", unlisted -> unlisted
                    .description("Set whether the map is listed")
                    .executes(playerOnly(this::handleSetListed), listedArg)
                    .examples("/map alter 123-456-789 listed true"))
                .child("version", version -> version
                    .description("Set the minimum required version for the map")
                    .executes(playerOnly(this::handleSetVersion), versionArg)
                    .examples("/map alter 123-456-789 version 1.21.7"))
            )
        );
    }

    private void handleSetType(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        var newType = context.get(typeArg);

        if (map == null) {
            player.sendMessage(
                Component.translatable("command.play.map_not_found", Component.text(context.getRaw(mapArg))));
            return;
        }
        if (map.settings().getVariant() == newType) {
            player.sendMessage("Map already has type " + newType);
            return;
        }

        map.settings().setVariant(newType);
        if (doMapUpdate(player, map)) {
            player.sendMessage("Map type set to " + newType);
        }
    }

    private void handleSetName(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        var newName = context.get(nameArg);

        if (map == null) {
            player.sendMessage(
                Component.translatable("command.play.map_not_found", Component.text(context.getRaw(mapArg))));
            return;
        }
        map.settings().setName(newName);
        if (doMapUpdate(player, map)) {
            player.sendMessage("Map name set to " + newName);
        }
    }

    private void handleSetDisplayItem(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        var newDisplayItem = context.get(displayItemArg);

        if (map == null) {
            player.sendMessage(
                Component.translatable("command.play.map_not_found", Component.text(context.getRaw(mapArg))));
            return;
        }
        map.settings().setIcon(newDisplayItem);
        if (doMapUpdate(player, map)) {
            player.sendMessage(Component.text("Map display item set to ").append(LanguageProviderV2.getVanillaTranslation(newDisplayItem)));
        }
    }

    private void handleSetSubVariant(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        var newSubVariant = context.get(subvariantArg);

        if (map == null) {
            player.sendMessage(
                Component.translatable("command.play.map_not_found", Component.text(context.getRaw(mapArg))));
            return;
        }
        try {
            if (map.settings().getVariant() == MapVariant.PARKOUR) {
                var subVariant = ParkourSubVariant.valueOf(newSubVariant.toUpperCase());
                map.settings().setParkourSubVariant(subVariant);
            } else {
                var subVariant = BuildingSubVariant.valueOf(newSubVariant.toUpperCase());
                map.settings().setBuildingSubVariant(subVariant);
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Invalid variant " + newSubVariant + " for map type " + map.settings().getVariant()));
            return;
        }
        if (doMapUpdate(player, map)) {
            player.sendMessage(Component.text("Map variant set to " + newSubVariant));
        }
    }

    private void handleSetSize(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        var newSize = context.get(sizeArg);

        if (map == null) {
            player.sendMessage(
                Component.translatable("command.play.map_not_found", Component.text(context.getRaw(mapArg))));
            return;
        }
        map.settings().setSize(newSize);
        if (doMapUpdate(player, map)) {
            player.sendMessage(Component.text("Map size set to " + newSize));
        }
    }

    private void handleAddTag(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        var tag = context.get(tagArg);

        if (map == null) {
            player.sendMessage(
                Component.translatable("command.play.map_not_found", Component.text(context.getRaw(mapArg))));
            return;
        }
        var added = map.settings().addTag(tag);
        if (!added) {
            player.sendMessage(Component.text("Map already has tag " + tag));
            return;
        }
        if (doMapUpdate(player, map)) {
            player.sendMessage(Component.text("Added tag " + tag));
        }
    }

    private void handleRemoveTag(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        var tag = context.get(tagArg);

        if (map == null) {
            player.sendMessage(
                Component.translatable("command.play.map_not_found", Component.text(context.getRaw(mapArg))));
            return;
        }
        var removed = map.settings().removeTag(tag);
        if (!removed) {
            player.sendMessage(Component.text("Map does not have tag " + tag));
            return;
        }
        if (doMapUpdate(player, map)) {
            player.sendMessage(Component.text("Removed tag " + tag));
        }
    }

    private void handleSetQuality(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        var newQuality = context.get(qualityArg);

        if (map == null) {
            player.sendMessage(
                Component.translatable("command.play.map_not_found", Component.text(context.getRaw(mapArg))));
            return;
        }
        map.settings().modifyUpdateRequest(req -> req.setQualityOverride(newQuality));
        if (doMapUpdate(player, map)) {
            player.sendMessage(Component.text("Set quality override to " + newQuality));
        }
    }

    private void handleSetSetting(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        var setting = context.get(settingsArg);
        var json = context.get(settingDataArg);

        if (map == null) {
            player.sendMessage(
                Component.translatable("command.play.map_not_found", Component.text(context.getRaw(mapArg))));
            return;
        }
        var result = setting.codec().decode(Transcoder.JSON, json);
        switch (result) {
            case Result.Ok(Object data) -> {
                writeSetting(map.settings(), setting, data);
                if (doMapUpdate(player, map)) {
                    player.sendMessage(Component.text("Set setting " + setting.key() + " to " + data));
                }
            }
            case Result.Error(String message) -> {
                player.sendMessage(Component.text("Invalid data for setting " + setting.key()));
                player.sendMessage(Component.text("Error: " + message));
            }
        }
    }

    private void handleSetListed(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        var listed = context.get(listedArg);

        if (map == null) {
            player.sendMessage(
                Component.translatable("command.play.map_not_found", Component.text(context.getRaw(mapArg))));
            return;
        }
        map.settings().modifyUpdateRequest(req -> req.setListed(listed));
        if (doMapUpdate(player, map)) {
            player.sendMessage(Component.text(listed ? "Set map to listed" : "Set map to unlisted"));
        }
    }

    private void handleSetVersion(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        var version = context.get(versionArg);

        if (map == null) {
            player.sendMessage(
                Component.translatable("command.play.map_not_found", Component.text(context.getRaw(mapArg))));
            return;
        }
        var protocolVersion = ProtocolVersions.getProtocolVersion(version);
        if (protocolVersion == -1) {
            player.sendMessage(Component.text("Unknown version " + version));
            return;
        }
        map.settings().modifyUpdateRequest(req -> req.setProtocolVersion(protocolVersion));
        if (doMapUpdate(player, map)) {
            player.sendMessage(Component.text("Set minimum required version to " + version));
        }
    }

    private <T> void writeSetting(@NotNull MapSettings settings, @NotNull MapSetting<T> setting, @NotNull Object data) {
        //noinspection unchecked
        settings.set(setting, (T) data);
    }

    /**
     * Updates the map data, returning true if the update was successful.
     * If not, the error will be handled and a relevant message will be sent to the player.
     */
    @Blocking
    private boolean doMapUpdate(@NotNull Player player, @NotNull MapData map) {
        try {
            map.settings().withUpdateRequest(req -> {
                maps.update(map.id(), req);
                return true; // Exceptions handled outside
            });
            return true;
        } catch (MapService.NotFoundError ignored) {
            player.sendMessage("Map not found");
        } catch (Exception e) {
            player.sendMessage(Component.translatable("generic.unknown_error"));
            ExceptionReporter.reportException(e, player);
        }
        return false;
    }

}
