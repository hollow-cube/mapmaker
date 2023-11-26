package net.hollowcube.mapmaker.hub.feature.misc;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.hub.HubServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.server.ServerTickMonitorEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.monitoring.BenchmarkManager;
import net.minestom.server.monitoring.TickMonitor;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.MathUtils;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public class CyberpunkStatDisplay implements Supplier<TaskSchedule> {
    private static final GlobalEventHandler EVENT_HANDLER = MinecraftServer.getGlobalEventHandler();
    private static final BenchmarkManager BENCHMARK_MANAGER = MinecraftServer.getBenchmarkManager();

    private static final double MAX_TICK_MS = 50;
    private static final double MAX_MEMORY_MB = 1024 * 3;
    private static final double MAX_WIDTH = 2.48;

    private final AtomicReference<TickMonitor> LAST_TICK = new AtomicReference<>();
    private final TextDisplayMeta leftText;
    private final TextDisplayMeta rightText;
    private final BlockDisplayMeta tickTimeBar;
    private final BlockDisplayMeta memoryUsageBar;

    public CyberpunkStatDisplay(@NotNull HubServer server) {
        EVENT_HANDLER.addListener(ServerTickMonitorEvent.class, event -> LAST_TICK.set(event.getTickMonitor()));
        BENCHMARK_MANAGER.enable(Duration.of(2, TimeUnit.SECOND));

        // Nice positions for toying locally
//        var staticTextPos = new Pos(-3 + 0.001, 41.15, 0.53, -90, 0);
//        var tickTimeBarPos = new Pos(-3 + 0.001, 42.1, 1.76);
//        var memoryUsageBarPos = new Pos(-3 + 0.001, 41.35, 1.76);

        // Real positions
        var staticTextPos = new Pos(-112.53, 72.15, -52.999);
        var tickTimeBarPos = new Pos(-113.76, 73.1, -52.998);
        var memoryUsageBarPos = new Pos(-113.76, 72.35, -52.998);

        leftText = createTextEntity(server.world().instance(), staticTextPos);
        rightText = createTextEntity(server.world().instance(), staticTextPos);
        tickTimeBar = createBarEntity(server.world().instance(), tickTimeBarPos);
        memoryUsageBar = createBarEntity(server.world().instance(), memoryUsageBarPos);
        // Add backgrounds for the bars
        createBarEntity(server.world().instance(), tickTimeBarPos.sub(0, 0, 0.001));
        createBarEntity(server.world().instance(), memoryUsageBarPos.sub(0, 0, 0.001));

        // Static title text, no need to update all the time
        leftText.setText(Component.text()
                .append(Component.text("ᴛɪᴄᴋ", TextColor.color(0x696969)))
                .appendNewline().appendNewline().appendNewline()
                .append(Component.text("ᴍᴇᴍᴏʀʏ", TextColor.color(0x696969)))

                .appendNewline().appendNewline()
                .append(Component.text(FontUtil.computeOffset(100)))
                .build());

        // Align right
        rightText.setAlignLeft(false);
        rightText.setAlignRight(true);
    }

    @Override
    public TaskSchedule get() {
        var tickMonitor = LAST_TICK.get();
        if (tickMonitor == null) return TaskSchedule.tick(40); // sanity

        var tickTimeMs = tickMonitor.getTickTime();
        var memoryUsageMb = BENCHMARK_MANAGER.getUsedMemory() / 1e6;

        // Update text
        rightText.setText(Component.text()
                .append(Component.text(MathUtils.round(tickTimeMs, 2) + "ms", TextColor.color(0xCCCCCC)))
                .appendNewline().appendNewline().appendNewline()
                .append(Component.text(MathUtils.round(memoryUsageMb, 2) + "MB", TextColor.color(0xCCCCCC)))

                // Static alignment
                .appendNewline().appendNewline()
                .append(Component.text(FontUtil.computeOffset(100)))
                .build());

        // Update maxs
        var tickTimeScaled = tickTimeMs / MAX_TICK_MS;
        tickTimeBar.setBlockState(percentToColor(tickTimeScaled));
        tickTimeBar.setScale(tickTimeBar.getScale().withX(MathUtils.clamp(tickTimeScaled * MAX_WIDTH, 0.001, MAX_WIDTH)));
        var memoryUsageScaled = memoryUsageMb / MAX_MEMORY_MB;
        memoryUsageBar.setBlockState(percentToColor(memoryUsageScaled));
        memoryUsageBar.setScale(memoryUsageBar.getScale().withX(MathUtils.clamp(memoryUsageScaled * MAX_WIDTH, 0.001, MAX_WIDTH)));

        return TaskSchedule.tick(40);
    }

    private int percentToColor(double value) {
        if (value < 0.6) {
            return Block.LIME_CONCRETE.stateId();
        } else if (value < 0.8) {
            return Block.YELLOW_CONCRETE.stateId();
        } else {
            return Block.RED_CONCRETE.stateId();
        }
    }

    private @NotNull TextDisplayMeta createTextEntity(@NotNull Instance instance, @NotNull Pos pos) {
        var entity = new Entity(EntityType.TEXT_DISPLAY) {{
            hasPhysics = false;
            setNoGravity(true);
        }};

        var meta = (TextDisplayMeta) entity.getEntityMeta();
        meta.setBackgroundColor(0);
        meta.setAlignLeft(true);
        meta.setAlignRight(false);
        entity.setInstance(instance, pos).join();
        return meta;
    }

    private @NotNull BlockDisplayMeta createBarEntity(@NotNull Instance instance, @NotNull Pos pos) {
        var entity = new Entity(EntityType.BLOCK_DISPLAY) {{
            hasPhysics = false;
            setNoGravity(true);
        }};

        var meta = (BlockDisplayMeta) entity.getEntityMeta();
        meta.setBlockState(Block.GRAY_CONCRETE.stateId());
        meta.setScale(new Vec(MAX_WIDTH, 0.3, 0.001));
        entity.setInstance(instance, pos).join();
        return meta;
    }
}
