package net.hollowcube.compat.lunar;

import com.google.auto.service.AutoService;
import net.hollowcube.compat.api.CompatProvider;
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
    private static final ClientboundLunarPacket MOD_SETTINGS = new ClientboundLunarPacket(
            Map.of(
                    "@type", ClientboundLunarPacket.TYPE_PREFIX + "configurable.v1.ConfigurableSettings",
                    "apollo_module", "mod_setting",
                    "enable", true,
                    // todo there is probably a cleaner and more configurable way to do this
                    "properties", Map.of(
                            "bossbar.enabled", false,
                            "saturation.enabled", false,
                            "direction-hud.enabled", false,
                            "armorstatus.enabled", false,
                            "title.enabled", false
                    )
            ));
    private static final ClientboundLunarPacket ENABLE_RICH_PRESENCE = new ClientboundLunarPacket(
            Map.of(
                    "@type", ClientboundLunarPacket.TYPE_PREFIX + "configurable.v1.ConfigurableSettings",
                    "apollo_module", "rich_presence",
                    "enable", true
            )
    );

    @Override
    public void registerListeners(GlobalEventHandler events) {
        events.addListener(PlayerPluginMessageEvent.class, this::handleLunarPacket);
    }

    @Override
    public void registerPackets(PacketRegistry registry) {
        registry.register(ClientboundLunarPacket.TYPE);
        registry.register(ServerboundLunarPacket.LUNAR_APOLLO_TYPE, (player, packet) -> {
        });
        registry.register(ServerboundLunarPacket.APOLLO_JSON_TYPE, (player, packet) -> {
        });

    }

    private void handleLunarPacket(PlayerPluginMessageEvent event) {
        if (event.getPlayer().hasTag(LUNAR_SUPPORT_ENABLED)) {
            return;
        }

        if (event.getIdentifier().equalsIgnoreCase("minecraft:register")) {
            if (event.getMessageString().contains("lunar:apollo") || event.getMessageString().contains("apollo:json")) {
                event.getPlayer().setTag(LUNAR_SUPPORT_ENABLED, true);
                MOD_SETTINGS.send(event.getPlayer());
                ENABLE_RICH_PRESENCE.send(event.getPlayer());
            }
        }
    }

    @Override
    public void setRichPresence(@NotNull Player player, @NotNull String playerState, @NotNull String gameName, @NotNull String gameVariantName) {
        new ClientboundLunarPacket(
                Map.of(
                        "@type", ClientboundLunarPacket.TYPE_PREFIX + "richpresence.v1.OverrideServerRichPresenceMessage",
                        "game_name", gameName,
                        // This is a lunar bug, it seems to escape the / character and discord doesn't undo it, so we need to replace it with another character
                        "game_variant_name", gameVariantName.replace("/", "∕"),
                        "player_state", playerState
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
