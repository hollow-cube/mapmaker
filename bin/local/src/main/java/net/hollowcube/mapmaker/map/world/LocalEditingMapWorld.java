package net.hollowcube.mapmaker.map.world;

import com.google.inject.Inject;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.local.LocalServerRunner;
import net.hollowcube.mapmaker.local.proj.Project;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.feature.FeatureList;
import net.hollowcube.terraform.Terraform;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;

public class LocalEditingMapWorld extends EditingMapWorld implements LocalProjectWorld {

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
    public @NotNull Project project() {
        var path = server().workspace().path().resolve(map().id()).resolve("mmproj.json");
        if (!Files.exists(path)) throw new RuntimeException("Project file not found: " + path);
        return Project.read(path);
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
