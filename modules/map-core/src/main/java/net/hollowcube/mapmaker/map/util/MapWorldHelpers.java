package net.hollowcube.mapmaker.map.util;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapPlayerTeleportingEvent;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerData;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.RelativeFlags;
import net.minestom.server.entity.attribute.Attribute;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class MapWorldHelpers {

    public static final UUID MAP_WORLD_RESOURCE_PACK_UUID = UUID.fromString("5225c051-ed7a-4315-bf4d-c75e000d029a");

    private MapWorldHelpers() {
    }

    public static void applyMapResourcePack(MapData map, Player player) {
        var pack = map.getSetting(MapSettings.RESOURCE_PACK);
        if (pack.isEmpty()) {
            player.removeResourcePacks(MAP_WORLD_RESOURCE_PACK_UUID);
        } else {
            var url = String.format("https://hollowcube-resource-pack.s3.amazonaws.com/%s.zip", pack);
            var request = ResourcePackRequest.resourcePackRequest()
                    .packs(ResourcePackInfo.resourcePackInfo(MAP_WORLD_RESOURCE_PACK_UUID, URI.create(url), pack))
                    .prompt(LanguageProviderV2.translate(Component.translatable("map.join.resource_pack.prompt")))
                    .required(true);

            player.sendResourcePacks(request);
        }
    }

    @NonBlocking
    public static void resetPlayerOnTickThread(@NotNull Player player) {
        resetPlayerOnTickThread(player, true);
    }

    @NonBlocking
    public static void resetPlayerOnTickThread(@NotNull Player player, boolean clearInventory) {
        player.refreshCommands();
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlying(false);
        player.setFlying(false);
        player.setFlyingSpeed(0.05f);
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20);
        player.setHealth(20);
        player.setInvisible(false);
        player.setVelocity(Vec.ZERO);
        player.clearEffects();
        if (clearInventory) player.getInventory().clear();
        player.updateViewerRule(null);
        player.getPlayerMeta().setFlyingWithElytra(false);
//        player.setPermissionLevel(4); TODO this enables operator commands on the client which is problematic, we need to find another way to do this

        // Reapply the cosmetics they have on
        var playerData = PlayerData.fromPlayer(player);
        MiscFunctionality.applyCosmetics(player, playerData);
    }

    public static CompletableFuture<Void> teleportPlayer(@NotNull Player player, @NotNull Point position) {
        var world = MapWorld.forPlayer(player);
        if (world != null) world.callEvent(new MapPlayerTeleportingEvent(world, player, position));
        return player.teleport(position.asPos(), Vec.ZERO, null, RelativeFlags.NONE);
    }

}
