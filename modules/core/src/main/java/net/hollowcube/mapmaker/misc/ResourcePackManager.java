package net.hollowcube.mapmaker.misc;

import net.hollowcube.common.ServerRuntime;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.event.player.PlayerResourcePackStatusEvent;
import net.minestom.server.resourcepack.ResourcePack;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ResourcePackManager {
    private static final Logger logger = LoggerFactory.getLogger(ResourcePackManager.class);
    private static final GlobalEventHandler EVENT_HANDLER = MinecraftServer.getGlobalEventHandler();

    private static final String RESOURCE_PACK_URL = "https://pub-620a83127bac451cbe2c402881b1b7d8.r2.dev/mapmaker-%s.zip";
    private static final UUID RESOURCE_PACK_ID = UUID.fromString("aceb326f-da15-45bc-bf2f-11940c21780c");

    /**
     * Sends the resource pack to the player, waiting for them to accept it.
     *
     * <p>This also checks with the proxy to confirm that they do not have the resource pack already.</p>
     *
     * @param player The player to send the resource pack to.
     * @return A future which completes when the resource pack is loaded for the player.
     */
    public static @NotNull CompletableFuture<Void> sendResourcePack(@NotNull Player player) {
        var runtime = ServerRuntime.getRuntime();
        if (runtime.resourcePackSha1().equals("dev")) {
            logger.info("Skipping resource pack for {} (in dev mode)", player.getUsername());
            return CompletableFuture.completedFuture(null);
        }
        String url = String.format(RESOURCE_PACK_URL, runtime.commit()), hash = runtime.resourcePackSha1();

        // Listen for the proxy response and send the proxy query
        final var future = new CompletableFuture<Boolean>();
        EVENT_HANDLER.addListener(EventListener.builder(PlayerPluginMessageEvent.class)
                .filter(event -> event.getIdentifier().equals("mapmaker:resource_pack"))
                .handler(event -> future.complete(Boolean.parseBoolean(event.getMessageString())))
                .expireCount(1).build());
        player.sendPluginMessage("mapmaker:resource_pack", hash);

        // Send the proxy query
        return future.thenCompose(doSend -> {
            if (!doSend) return CompletableFuture.completedFuture(null);
            logger.info("Sending resource pack {} ({}) to {}", url, hash, player.getUsername());

            // Send the pack and listen for the status
            var future2 = new CompletableFuture<Void>();
            EVENT_HANDLER.addListener(EventListener.builder(PlayerResourcePackStatusEvent.class)
                    .filter(event -> event.getPlayer() == player) //todo filter on the ID when its added to the event
                    .handler(event -> {
                        switch (event.getStatus()) {
                            case SUCCESSFULLY_LOADED -> future2.complete(null);
                            case ACCEPTED, DOWNLOADED -> {/* Indeterminate so do nothing */}
                            default -> {
                                // Any other case is bad so kick the player
                                logger.warn("Resource pack failed to load for {}", player.getUsername());
                                future2.complete(null);
                                player.kick(Component.text("Resource pack failed to load"));
                            }
                        }
                    })
                    .expireWhen(unused -> future2.isDone()).build());
            player.setResourcePack(ResourcePack.forced(RESOURCE_PACK_ID, url, hash));

            return future2;
        });
    }
}
