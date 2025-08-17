package net.hollowcube.mapmaker.map.feature.play.setting;


import com.google.auto.service.AutoService;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.util.MapWorldHelpers;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.UUID;

@AutoService(FeatureProvider.class)
public class ResourcePackFeatureProvider implements FeatureProvider {

    private static final UUID PACK_UUID = MapWorldHelpers.MAP_WORLD_RESOURCE_PACK_UUID;

    @Override
    public void configurePlayer(@NotNull MapWorld world, @NotNull Player player) {
        var pack = world.map().getSetting(MapSettings.RESOURCE_PACK);
        if (pack.isEmpty()) {
            player.removeResourcePacks(PACK_UUID);
        } else {
            var url = String.format("https://hollowcube-resource-pack.s3.amazonaws.com/%s.zip", pack);
            var request = ResourcePackRequest.resourcePackRequest()
                    .packs(ResourcePackInfo.resourcePackInfo(PACK_UUID, URI.create(url), pack))
                    .prompt(LanguageProviderV2.translate(Component.translatable("map.join.resource_pack.prompt")))
                    .required(true);

            player.sendResourcePacks(request);
        }
    }
}
