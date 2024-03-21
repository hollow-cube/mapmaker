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
    private final Argument<String> nameArg = Argument.GreedyString("name");
    private final Argument<Material> displayItemArg = Argument.Material("item");
    private final Argument<String> subvariantArg;
    private final Argument<MapSize> sizeArg = Argument.Enum("size", MapSize.class);
    private final Argument<MapTags.Tag> tagArg = Argument.Enum("tag", MapTags.Tag.class);
    private final Argument<MapQuality> qualityArg = Argument.Enum("quality", MapQuality.class);

    private final MapService mapService;
    private final PermManager permManager;

    public MapAlterCommand(@NotNull MapService mapService, PermManager permManager) {
        this.mapService = mapService;
        this.permManager = permManager;

        mapArg = CoreArgument.PlayableMap("map", mapService); //todo should be any map dependent on context.

        var subvariantTypes = new ArrayList<String>();
        for (var bsv : BuildingSubVariant.values())
            subvariantTypes.add(bsv.name().toLowerCase());
        for (var psv : ParkourSubVariant.values())
            subvariantTypes.add(psv.name().toLowerCase());
        subvariantArg = Argument.Word("variant").with(subvariantTypes);
    }

    public void build(@NotNull CommandBuilder builder) {
        builder.child("alter", root -> root
                .condition(permManager.createPlatformCondition2(PlatformPerm.MAP_ADMIN))
                .child(mapArg, alter -> alter
                                .child("name", name -> name.executes(playerOnly(this::handleSetName), nameArg))
                                .child("displayItem", di -> di.executes(playerOnly(this::handleSetDisplayItem), displayItemArg))
                                .child("variant", sv -> sv.executes(playerOnly(this::handleSetSubVariant), subvariantArg))
                                .child("size", size -> size.executes(playerOnly(this::handleSetSize), sizeArg))
                                .child("tag", tag -> tag
                                        .child("add", add -> add.executes(playerOnly(this::handleAddTag), tagArg))
                                        .child("remove", rem -> rem.executes(playerOnly(this::handleRemoveTag), tagArg)))
                                .child("quality", quality -> quality.executes(playerOnly(this::handleSetQuality), qualityArg))
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
