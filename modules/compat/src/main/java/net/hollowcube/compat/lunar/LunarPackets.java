package net.hollowcube.compat.lunar;

import net.hollowcube.compat.lunar.packets.ClientboundLunarPacket;

import java.util.Map;
import java.util.UUID;

public class LunarPackets {

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

    public static ClientboundLunarPacket getModSettingsPacket() {
        return MOD_SETTINGS;
    }

    public static ClientboundLunarPacket getEnableRichPresencePacket() {
        return ENABLE_RICH_PRESENCE;
    }

    public static ClientboundLunarPacket getRequestModsPacket() {
        return new ClientboundLunarPacket(Map.of(
            "@type", ClientboundLunarPacket.TYPE_PREFIX + "modsetting.v1.InstalledModsRequest",
            "request_id", UUID.randomUUID().toString()
        ));
    }
}
