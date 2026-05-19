package net.hollowcube.compat.lunar;

import com.google.auto.service.AutoService;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.api.ModChannelRegisterEvent;
import net.hollowcube.compat.api.discord.DiscordRichPresenceProvider;
import net.hollowcube.compat.api.packet.PacketRegistry;
import net.hollowcube.compat.lunar.events.LunarPlayerInitEvent;
import net.hollowcube.compat.lunar.packets.ClientboundLunarPacket;
import net.hollowcube.compat.lunar.packets.ServerboundLunarPacket;
import net.hollowcube.compat.lunar.payload.InstalledModsResponsePayload;
import net.hollowcube.compat.lunar.payload.LunarPayload;
import net.hollowcube.compat.lunar.payload.PaginatedPayloadHandler;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


@AutoService({CompatProvider.class, DiscordRichPresenceProvider.class})
public class LunarCompatProvider implements CompatProvider, DiscordRichPresenceProvider {

    public static final Tag<Boolean> LUNAR_SUPPORT_ENABLED = Tag.Transient("mapmaker:lunar/enabled");
    private static final PaginatedPayloadHandler PAGINATED_PAYLOAD_HANDLER = new PaginatedPayloadHandler();

    @Override
    public void registerListeners(GlobalEventHandler events) {
        events.addListener(ModChannelRegisterEvent.class, this::handleLunarPacket);
    }

    @Override
    public void registerPackets(PacketRegistry registry) {
        registry.register(ClientboundLunarPacket.TYPE);
        registry.register(ServerboundLunarPacket.APOLLO_JSON_TYPE, (player, packet) -> {
            var payload = packet.payload();

            if (payload instanceof LunarPayload.Paginated<?> paginated) {
                payload = PAGINATED_PAYLOAD_HANDLER.handle(paginated);
            }

            if (payload != null) {
                handleLunarPayload(player, payload);
            }
        });
    }

    private void handleLunarPacket(ModChannelRegisterEvent event) {
        if (!event.getChannels().contains("lunar:apollo")) return;
        if (event.getPlayer().hasTag(LUNAR_SUPPORT_ENABLED)) return;

        event.getPlayer().setTag(LUNAR_SUPPORT_ENABLED, true);
        LunarPackets.getModSettingsPacket().send(event.getPlayer());
        LunarPackets.getEnableRichPresencePacket().send(event.getPlayer());
        LunarPackets.getRequestModsPacket().send(event.getPlayer());
    }

    private void handleLunarPayload(Player player, LunarPayload payload) {
        if (payload instanceof InstalledModsResponsePayload response) {
            EventDispatcher.call(new LunarPlayerInitEvent(player, response));
        }
    }

    @Override
    public void setRichPresence(
        @NotNull Player player,
        @NotNull String activity, @NotNull String name,
        @Nullable String details
    ) {
        // This is a lunar bug, it seems to escape the / character and discord doesn't undo it
        details = details != null ? details.replace("/", "∕") : "";

        new ClientboundLunarPacket(
            Map.of(
                "@type", ClientboundLunarPacket.TYPE_PREFIX + "richpresence.v1.OverrideServerRichPresenceMessage",
                "player_state", activity,
                "game_name", name,
                "game_variant_name", details
            )
        ).send(player);
    }

    @Override
    public void clearRichPresence(@NotNull Player player) {
        new ClientboundLunarPacket(
            Map.of(
                "@type", ClientboundLunarPacket.TYPE_PREFIX + "richpresence.v1.ResetServerRichPresenceMessage"
            )
        ).send(player);
    }

    @Override
    public boolean isRichPresenceSupportedFor(@NotNull Player player) {
        return player.hasTag(LUNAR_SUPPORT_ENABLED);
    }
}
