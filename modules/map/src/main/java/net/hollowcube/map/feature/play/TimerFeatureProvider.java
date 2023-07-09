package net.hollowcube.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.map.event.MapWorldCompleteEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
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
                .repeat(1, net.minestom.server.utils.time.TimeUnit.SERVER_TICK)
                .schedule();

        world.addScopedEventNode(EventNode.type("balwhdf", EventFilter.INSTANCE)
                .addListener(MapWorldCompleteEvent.class, this::onComplete));

        return true;
    }

    public void onComplete(@NotNull MapWorldCompleteEvent event) {
        var player = event.getPlayer();
        sendPlaytime(player, event.getMapWorld().instance().getWorldAge());
    }

    private void sendTimerActionBar(@NotNull MapWorld world) {
        var worldTime = world.instance().getWorldAge();

        for (var player : world.players()) {
            sendPlaytime(player, worldTime);
        }
    }

    private void sendPlaytime(@NotNull Player player, long worldTime) {
        var text = new StringBuilder();
        text.append(TIMER_CONTAINER.fontChar());
        text.append(FontUtil.computeOffset(-52));

        var saveState = SaveState.fromPlayer(player);
        var timeMs = saveState.getPlaytime(worldTime);

        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeMs) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeMs));
        long milliseconds = timeMs - TimeUnit.MINUTES.toMillis(minutes) - TimeUnit.SECONDS.toMillis(seconds);

        var a = String.format("%02d:%02d.%03d", minutes, seconds, milliseconds);
        text.append(a);

        player.sendActionBar(Component.text(text.toString(), TextColor.color(78, 92, 36)));
    }
}
