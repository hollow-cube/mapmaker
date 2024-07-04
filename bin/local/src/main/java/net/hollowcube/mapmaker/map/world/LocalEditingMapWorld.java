package net.hollowcube.mapmaker.map.world;

import com.google.inject.Inject;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.local.LocalServerRunner;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.feature.FeatureList;
import net.hollowcube.terraform.Terraform;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LocalEditingMapWorld extends EditingMapWorld {

    @Inject
    public LocalEditingMapWorld(
            @NotNull MapServer server,
            @NotNull Terraform terraform,
            @NotNull FeatureList features,
            @NotNull MapData map
    ) {
        super(server, terraform, features, map);
    }

    @Override
    public @NotNull LocalServerRunner server() {
        return (LocalServerRunner) super.server();
    }

    @Override
    protected @NotNull TestingMapWorld createTestingWorld() {
        return new LocalTestingMapWorld(this);
    }

    @Override
    protected @Nullable BossBar buildBossBarLine1(@NotNull Player player) {
        var text = Component.text(FontUtil.rewrite("bossbar_small_1", "mapmaker local editor. not for distribution."), FontUtil.NO_SHADOW);
        return BossBar.bossBar(text, 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
    }

    @Override
    protected @Nullable BossBar buildBossBarLine2(@NotNull Player player) {
        return null;
    }
}
