package net.hollowcube.mapmaker.map.world;

import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.entity.potion.PotionHandler;
import net.hollowcube.mapmaker.map.feature.FeatureList;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.misc.BossBars;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.hollowcube.mapmaker.map.feature.play.item.SetSpectatorCheckpointItem.SPECTATOR_CHECKPOINT;

public abstract class AbstractMapMakerMapWorld extends AbstractMapWorld {
    protected static final Tag<Boolean> FIRST_JOIN_TAG = Tag.Boolean("mapworld.firstjoin");

    private final FeatureList features;
    private List<FeatureProvider> enabledFeatures;

    protected AbstractMapMakerMapWorld(
            @NotNull MapServer server, @NotNull MapData map,
            @NotNull FeatureList features, @NotNull MapInstance instance) {
        super(server, map, instance);

        this.features = features;

        // Add support for adding and removing potion effects
        eventNode().addChild(PotionHandler.EVENT_NODE);
    }

    public @NotNull FeatureList features() {
        return features;
    }

    public @NotNull List<FeatureProvider> enabledFeatures() {
        return enabledFeatures;
    }

    @Override
    public void load() {
        super.load();

        this.enabledFeatures = features.loadMap(this);
    }

    @Override
    public void close(@Nullable Component reason) {
        this.enabledFeatures.forEach(fp -> fp.cleanupMap(this));
        this.enabledFeatures = null;

        super.close(reason);
    }

    @Override
    public void addPlayer(@NotNull Player player) {
        super.addPlayer(player);

        sendBossBars(player);
    }

    @Override
    public void removePlayer(@NotNull Player player) {
        player.removeTag(SPECTATOR_CHECKPOINT);

        super.removePlayer(player);
    }

    protected void sendBossBars(@NotNull Player player) {
        //todo colors here should use our known colors
        BossBars.clear(player);
        final BossBar line1 = buildBossBarLine1(player);
        if (line1 != null) player.showBossBar(line1);
        final BossBar line2 = buildBossBarLine2(player);
        if (line2 != null) player.showBossBar(line2);
    }

    protected @Nullable BossBar buildBossBarLine1(@NotNull Player player) {
        return null;
    }

    protected @Nullable BossBar buildBossBarLine2(@NotNull Player player) {
        return BossBars.ADDRESS_LINE;
    }
}
