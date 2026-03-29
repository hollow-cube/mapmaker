package net.hollowcube.compat.lunar;

import com.google.auto.service.AutoService;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.api.ModChannelRegisterEvent;
import net.hollowcube.compat.api.discord.DiscordRichPresenceProvider;
import net.hollowcube.compat.api.packet.PacketRegistry;
import net.hollowcube.compat.lunar.packets.ClientboundLunarPacket;
import net.hollowcube.compat.lunar.packets.ServerboundLunarPacket;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Map;


@AutoService({CompatProvider.class, DiscordRichPresenceProvider.class})
public class LunarCompatProvider implements CompatProvider, DiscordRichPresenceProvider {
    public static final Tag<Boolean> LUNAR_SUPPORT_ENABLED = Tag.Transient("mapmaker:lunar/enabled");
    private static final ClientboundLunarPacket MOD_SETTINGS = new ClientboundLunarPacket(Map.of(
        "@type", ClientboundLunarPacket.TYPE_PREFIX + "configurable.v1.ConfigurableSettings",
        "apollo_module", "mod_setting",
        "enable", true,
        "properties", Map.of(
            "replaymod.enabled", true,
            "minimap.enabled", false,
            "weather-changer.enabled", false,
            "day-counter.enabled", false,
            "bossbar.enabled", false,
            "saturation.enabled", false,
            "direction-hud.enabled", false,
            "armorstatus.enabled", false,
            "titles.enabled", false
        )
    ));
    private static final ClientboundLunarPacket ENABLE_RICH_PRESENCE = new ClientboundLunarPacket(Map.of(
        "@type", ClientboundLunarPacket.TYPE_PREFIX + "configurable.v1.ConfigurableSettings",
        "apollo_module", "rich_presence",
        "enable", true
    ));

    @Override
    public void registerListeners(GlobalEventHandler events) {
        events.addListener(ModChannelRegisterEvent.class, this::handleLunarPacket);
    }

    @Override
    public void registerPackets(PacketRegistry registry) {
        registry.register(ClientboundLunarPacket.TYPE);
        registry.register(ServerboundLunarPacket.APOLLO_JSON_TYPE, (player, packet) -> {
            System.out.println("Received lunar json packet: " + packet.message());
        });
    }

    private void handleLunarPacket(ModChannelRegisterEvent event) {
        if (!event.getChannels().contains("lunar:apollo")) return;
        if (event.getPlayer().hasTag(LUNAR_SUPPORT_ENABLED)) return;

        event.getPlayer().setTag(LUNAR_SUPPORT_ENABLED, true);
        MOD_SETTINGS.send(event.getPlayer());
        ENABLE_RICH_PRESENCE.send(event.getPlayer());
    }

    @Override
    public void setRichPresence(
        @NotNull Player player,
        @NotNull String activity,
        @NotNull String map
    ) {
        new ClientboundLunarPacket(
            Map.of(
                "@type", ClientboundLunarPacket.TYPE_PREFIX + "richpresence.v1.OverrideServerRichPresenceMessage",
                "game_name", "game_name",
                "game_variant_name", "game_variant_name",
                "game_state", "game_state",
                "player_state", "player_state",
                "map_name", map,
                "sub_server", "sub_server"
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
