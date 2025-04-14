package net.hollowcube.compat.feather;

import com.google.auto.service.AutoService;
import net.digitalingot.feather.serverapi.messaging.*;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CHandshake;
import net.digitalingot.feather.serverapi.messaging.messages.client.S2CSetDiscordActivity;
import net.digitalingot.feather.serverapi.messaging.messages.server.C2SClientHello;
import net.digitalingot.feather.serverapi.messaging.messages.server.C2SHandshake;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.api.discord.DiscordRichPresenceProvider;
import net.hollowcube.compat.api.packet.PacketRegistry;
import net.hollowcube.compat.feather.packets.ClientboundFeatherPacket;
import net.hollowcube.compat.feather.packets.ServerboundFeatherPacket;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.tag.Tag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@AutoService({CompatProvider.class, DiscordRichPresenceProvider.class})
public class FeatherCompatProvider implements CompatProvider, DiscordRichPresenceProvider {
    private static final String IMAGE_URL = "https://servermappings.lunarclientcdn.com/logos/hollowcube.png";
    private static final String FEATHER_CHANNEL = "feather:client";
    private static final String FEATHER_CHANNEL_FRAGMENTED = FEATHER_CHANNEL + "/frag";
    private static final Tag<Boolean> FEATHER_SUPPORT_ENABLED = Tag.Transient("mapmaker:feather/enabled");
    private static final Handshaking HANDSHAKING = new Handshaking();

    public static void sendMessage(final Player player, final Message<ClientMessageHandler> message) {
        final var bytes = MessageEncoder.CLIENT_BOUND.encode(message);
        if (bytes.length > 1048576 /* 1MiB */) {
            for (byte[] data : MessageFragmenter.CLIENT_BOUND.fragment(message)) {
                player.sendPluginMessage(FEATHER_CHANNEL_FRAGMENTED, data);
            }
        } else {
            player.sendPluginMessage(FEATHER_CHANNEL, bytes);
        }
    }

    @Override
    public void registerListeners(GlobalEventHandler events) {
        events.addListener(PlayerDisconnectEvent.class, e -> HANDSHAKING.finish(e.getPlayer()));

    }

    @Override
    public void registerPackets(PacketRegistry registry) {
        registry.register(ClientboundFeatherPacket.TYPE);
        registry.register(ServerboundFeatherPacket.TYPE, (player, packet) -> {
            if (!player.hasTag(FEATHER_SUPPORT_ENABLED)) {
                final var hello = HANDSHAKING.handle(player, packet.message());

                if (hello != null) {
                    player.setTag(FEATHER_SUPPORT_ENABLED, true);
                }

            }
        });
    }

    @Override
    public void setRichPresence(Player player, String gameName, String gameVariantName, String playerState) {
        // this is probably turbo trash, but it creates consistency between Lunar and Feather
        final var details = playerState + " " + gameName + " on Hollow Cube";
        sendMessage(player, new S2CSetDiscordActivity(IMAGE_URL, "Hollow Cube", gameVariantName, details, null, null, null, null));
    }

    @Override
    public boolean isRichPresenceSupportedFor(Player player) {
        return player.hasTag(FEATHER_SUPPORT_ENABLED);
    }

    // This is directly copied from https://github.com/FeatherMC/feather-server-api/blob/main/bukkit/src/main/java/net/digitalingot/feather/serverapi/bukkit/messaging/BukkitMessagingService.java
    // but with some modifications to make it work with Minestom
    private static class Handshaking {
        private final Map<UUID, HandshakeState> handshakes = new HashMap<>();


        private HandshakeState getState(Player player) {
            return this.handshakes.getOrDefault(player.getUuid(), HandshakeState.EXPECTING_HANDSHAKE);
        }

        private void setState(UUID playerId, HandshakeState state) {
            this.handshakes.put(playerId, state);
        }

        private void accept(Player player) {
            setState(player.getUuid(), HandshakeState.EXPECTING_HELLO);
            sendMessage(player, new S2CHandshake());
        }

        private void reject(Player player) {
            setState(player.getUuid(), HandshakeState.REJECTED);
        }

        private void finish(Player player) {
            this.handshakes.remove(player.getUuid());
        }

        private C2SClientHello handle(Player player, Message<ServerMessageHandler> message) {
            HandshakeState state = getState(player);

            if (state == HandshakeState.REJECTED) {
                return null;
            }


            if (state == HandshakeState.EXPECTING_HANDSHAKE) {
                if (handleExpectingHandshake(message)) {
                    accept(player);
                } else {
                    reject(player);
                }
            } else if (state == HandshakeState.EXPECTING_HELLO) {
                if ((message instanceof C2SClientHello)) {
                    finish(player);
                    return (C2SClientHello) message;
                }
                reject(player);
            }

            return null;
        }

        private boolean handleExpectingHandshake(Message<ServerMessageHandler> message) {
            if (!(message instanceof C2SHandshake handshake)) {
                return false;
            }
            int protocolVersion = handshake.getProtocolVersion();
            if (protocolVersion > MessageConstants.VERSION) {
                // todo this doesn't actually do anything
            }
            return true;
        }


        private enum HandshakeState {
            EXPECTING_HANDSHAKE,
            EXPECTING_HELLO,
            REJECTED
        }
    }

}
