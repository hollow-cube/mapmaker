package net.hollowcube.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@AutoService(FeatureProvider.class)
public class TimerFeatureProvider implements FeatureProvider {
    private static final BadSprite TIMER_CONTAINER = BadSprite.SPRITE_MAP.get("hud/timer_container");

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if ((world.flags() & MapWorld.FLAG_PLAYING) == 0)
            return false;

        var settings = world.map().settings();
        if (settings.getVariant() != MapVariant.PARKOUR) return false;

        var instance = world.instance();
        instance.scheduler()
                .buildTask(() -> sendTimerActionBar(world))
                .repeat(2, net.minestom.server.utils.time.TimeUnit.SERVER_TICK)
                .schedule();

        return true;
    }

    private void sendTimerActionBar(@NotNull MapWorld world) {
        for (var player : world.players()) {
            var text = new StringBuilder();
            text.append(TIMER_CONTAINER.fontChar());
            text.append(FontUtil.computeOffset(-52));

            var saveState = SaveState.fromPlayer(player);
            var time = saveState.getPlaytime() + System.currentTimeMillis() - saveState.getPlayStartTime();

            long minutes = TimeUnit.MILLISECONDS.toMinutes(time);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time));
            long milliseconds = time - TimeUnit.MINUTES.toMillis(minutes) - TimeUnit.SECONDS.toMillis(seconds);

            var a = String.format("%02d:%02d.%03d", minutes, seconds, milliseconds);
            text.append(a);

            player.sendActionBar(Component.text(text.toString(), TextColor.color(78, 92, 36)));
        }
    }
}
