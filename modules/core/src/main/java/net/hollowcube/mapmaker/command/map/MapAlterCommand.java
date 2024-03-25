package net.hollowcube.mapmaker.command.map;

import net.hollowcube.command.CommandBuilder;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static net.hollowcube.command.dsl.CommandDsl.playerOnly;

/**
 * Notably not using {@link net.hollowcube.command.dsl.CommandDsl}, it doesn't support arguments followed by "subcommands" very well.
 */
public class MapAlterCommand {

    private final Argument<MapData> mapArg;
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

    private final MapService mapService;
    private final PermManager permManager;

    public MapAlterCommand(@NotNull MapService mapService, PermManager permManager) {
        this.mapService = mapService;
        this.permManager = permManager;

        mapArg = CoreArgument.PlayableMap("map", mapService) //todo should be any map dependent on context.
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
                .condition(permManager.createPlatformCondition2(PlatformPerm.MAP_ADMIN))
                .description("Edit information related to a map")
                .child(mapArg, alter -> alter
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
                        // todo toggle settings?
                ));
    }

    private void handleSetName(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        var newName = context.get(nameArg);

        map.settings().setName(newName);
        if (doMapUpdate(player, map)) {
            player.sendMessage("Map name set to " + newName);
        }
    }

    private void handleSetDisplayItem(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        var newDisplayItem = context.get(displayItemArg);

        map.settings().setIcon(newDisplayItem);
        if (doMapUpdate(player, map)) {
            player.sendMessage(Component.text("Map display item set to ").append(LanguageProviderV2.getVanillaTranslation(newDisplayItem)));
        }
    }

    private void handleSetSubVariant(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        var newSubVariant = context.get(subvariantArg);

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

        map.settings().setSize(newSize);
        if (doMapUpdate(player, map)) {
            player.sendMessage(Component.text("Map size set to " + newSize));
        }
    }

    private void handleAddTag(@NotNull Player player, @NotNull CommandContext context) {
        var map = context.get(mapArg);
        var tag = context.get(tagArg);

        var added = map.settings().removeTag(tag);
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

        map.settings().modifyUpdateRequest(req -> req.setQualityOverride(newQuality));
        if (doMapUpdate(player, map)) {
            player.sendMessage(Component.text("Set quality override to " + newQuality));
        }
    }

    /**
     * Updates the map data, returning true if the update was successful.
     * If not, the error will be handled and a relevant message will be sent to the player.
     */
    @Blocking
    private boolean doMapUpdate(@NotNull Player player, @NotNull MapData map) {
        try {
            var playerId = PlayerDataV2.fromPlayer(player).id();
            map.settings().withUpdateRequest(req -> {
                mapService.updateMap(playerId, map.id(), req);
                return true; // Exceptions handled outside
            });
            return true;
        } catch (MapService.NotFoundError ignored) {
            player.sendMessage("Map not found");
        } catch (Exception e) {
            player.sendMessage(Component.translatable("generic.unknown_error"));
            MinecraftServer.getExceptionManager().handleException(e);
        }
        return false;
    }

}
