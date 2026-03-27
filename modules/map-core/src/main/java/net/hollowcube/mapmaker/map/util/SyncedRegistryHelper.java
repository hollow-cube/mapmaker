package net.hollowcube.mapmaker.map.util;

import net.hollowcube.mapmaker.map.MapWorld;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.network.packet.server.CachedPacket;
import net.minestom.server.network.packet.server.common.TagsPacket;
import net.minestom.server.network.packet.server.configuration.SelectKnownPacksPacket;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.registry.Registries;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@NotNullByDefault
public final class SyncedRegistryHelper {
    private static final Logger logger = LoggerFactory.getLogger(SyncedRegistryHelper.class);

    private static final CachedPacket TEMP_TAGS_PACKET = new CachedPacket(SyncedRegistryHelper::createTagsPacket);

    public static void sendSyncedRegistries(MapWorld world, AsyncPlayerConfigurationEvent event) {
        final var player = event.getPlayer();

        List<SelectKnownPacksPacket.Entry> knownPacks;
        try {
            knownPacks = stealKnownPacksFuture(player).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            logger.warn("Client failed to respond to known packs request", e);
            knownPacks = null;
        } catch (ExecutionException e) {
            throw new RuntimeException("Error receiving known packs", e);
        }
        boolean excludeVanilla = knownPacks != null && knownPacks.contains(SelectKnownPacksPacket.MINECRAFT_CORE);

        // Send registry data ourself to allow custom biomes per map
        var serverProcess = MinecraftServer.process();
        player.sendPacket(serverProcess.chatType().registryDataPacket(serverProcess, excludeVanilla));
        player.sendPacket(serverProcess.dimensionType().registryDataPacket(serverProcess, excludeVanilla));
        player.sendPacket(world.biomes().registryDataPacket(excludeVanilla));
        player.sendPacket(serverProcess.damageType().registryDataPacket(serverProcess, excludeVanilla));
        player.sendPacket(serverProcess.dialog().registryDataPacket(serverProcess, excludeVanilla));
        player.sendPacket(serverProcess.trimMaterial().registryDataPacket(serverProcess, excludeVanilla));
        player.sendPacket(serverProcess.trimPattern().registryDataPacket(serverProcess, excludeVanilla));
        player.sendPacket(serverProcess.bannerPattern().registryDataPacket(serverProcess, excludeVanilla));
        player.sendPacket(serverProcess.enchantment().registryDataPacket(serverProcess, excludeVanilla));
        player.sendPacket(serverProcess.paintingVariant().registryDataPacket(serverProcess, excludeVanilla));
        player.sendPacket(serverProcess.jukeboxSong().registryDataPacket(serverProcess, excludeVanilla));
        player.sendPacket(serverProcess.instrument().registryDataPacket(serverProcess, excludeVanilla));
        player.sendPacket(serverProcess.wolfVariant().registryDataPacket(serverProcess, excludeVanilla));
        player.sendPacket(serverProcess.wolfSoundVariant().registryDataPacket(serverProcess, excludeVanilla));
        player.sendPacket(serverProcess.catVariant().registryDataPacket(serverProcess, excludeVanilla));
        player.sendPacket(serverProcess.chickenVariant().registryDataPacket(serverProcess, excludeVanilla));
        player.sendPacket(serverProcess.cowVariant().registryDataPacket(serverProcess, excludeVanilla));
        player.sendPacket(serverProcess.frogVariant().registryDataPacket(serverProcess, excludeVanilla));
        player.sendPacket(serverProcess.pigVariant().registryDataPacket(serverProcess, excludeVanilla));

        player.sendPacket(TEMP_TAGS_PACKET);
        event.setSendRegistryData(false);
    }

    private static TagsPacket createTagsPacket() {
        final List<TagsPacket.Registry> entries = new ArrayList<>();

        // The following are the registries which contain tags used by the vanilla client.
        // We don't care about registries unused by the client.
        final Registries registries = MinecraftServer.process();
        entries.add(registries.bannerPattern().tagRegistry());
        // TODO: our registry should handle this.
        entries.add(registries.biome().tagRegistry());
        entries.add(registries.blocks().tagRegistry());
        entries.add(registries.dialog().tagRegistry());
        entries.add(registries.catVariant().tagRegistry());
        entries.add(registries.damageType().tagRegistry());
        entries.add(registries.enchantment().tagRegistry());
        entries.add(registries.entityType().tagRegistry());
        entries.add(registries.fluid().tagRegistry());
        entries.add(registries.gameEvent().tagRegistry());
        entries.add(registries.instrument().tagRegistry());
        entries.add(registries.material().tagRegistry());
        entries.add(registries.paintingVariant().tagRegistry());

        return new TagsPacket(entries);
    }

    @SuppressWarnings("unchecked")
    private static CompletableFuture<List<SelectKnownPacksPacket.Entry>> stealKnownPacksFuture(Player player) {
        class Holder {
            static @UnknownNullability MethodHandle knownPacksFutureGetter;
        }
        try {
            if (Holder.knownPacksFutureGetter == null) {
                Holder.knownPacksFutureGetter = MethodHandles.privateLookupIn(PlayerConnection.class, MethodHandles.lookup())
                        .findGetter(PlayerConnection.class, "knownPacksFuture", CompletableFuture.class);
            }
            Object future = Holder.knownPacksFutureGetter.invokeExact(player.getPlayerConnection());
            if (future == null) {
                // There is a race here which could result in the client responding too quickly. In that case we need to re request
                // the known packs. todo: find a better way around this, its really cursed.
                return player.getPlayerConnection().requestKnownPacks(List.of(SelectKnownPacksPacket.MINECRAFT_CORE));
            }
            return (CompletableFuture<List<SelectKnownPacksPacket.Entry>>) future;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
