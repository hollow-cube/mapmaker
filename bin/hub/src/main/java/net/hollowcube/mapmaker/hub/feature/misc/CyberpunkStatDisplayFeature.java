package net.hollowcube.mapmaker.hub.feature.misc;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.map.MapServer;
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
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.MathUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.concurrent.atomic.AtomicReference;

@AutoService(HubFeature.class)
@SuppressWarnings("UnstableApiUsage")
public class CyberpunkStatDisplayFeature implements HubFeature {
    private static final GlobalEventHandler EVENT_HANDLER = MinecraftServer.getGlobalEventHandler();
    private static final BenchmarkManager BENCHMARK_MANAGER = MinecraftServer.getBenchmarkManager();

    private static final double MAX_TICK_MS = 50;
    private static final double MAX_MEMORY_MB = 1024 * 3;
    private static final double MAX_WIDTH = 2.48;

    private final AtomicReference<@Nullable TickMonitor> LAST_TICK = new AtomicReference<>();
    private @UnknownNullability TextDisplayMeta leftText; // lateinit
    private @UnknownNullability TextDisplayMeta rightText; // lateinit
    private @UnknownNullability BlockDisplayMeta tickTimeBar; // lateinit
    private @UnknownNullability BlockDisplayMeta memoryUsageBar; // lateinit

    @Override
    public void load(MapServer server, HubMapWorld world) {
        EVENT_HANDLER.addListener(ServerTickMonitorEvent.class, event -> LAST_TICK.set(event.getTickMonitor()));

        // Nice positions for toying locally
//        var staticTextPos = new Pos(-3 + 0.001, 41.15, 0.53, -90, 0);
//        var tickTimeBarPos = new Pos(-3 + 0.001, 42.1, 1.76);
//        var memoryUsageBarPos = new Pos(-3 + 0.001, 41.35, 1.76);

        // Real positions
        var staticTextPos = new Pos(-112.53 - 32, 72.15 - 9, -52.999 - 20);
        var tickTimeBarPos = new Pos(-113.76 - 32, 73.1 - 9, -52.998 - 20);
        var memoryUsageBarPos = new Pos(-113.76 - 32, 72.35 - 9, -52.998 - 20);

        leftText = createTextEntity(world.instance(), staticTextPos);
        rightText = createTextEntity(world.instance(), staticTextPos);
        tickTimeBar = createBarEntity(world.instance(), tickTimeBarPos);
        memoryUsageBar = createBarEntity(world.instance(), memoryUsageBarPos);
        // Add backgrounds for the bars
        createBarEntity(world.instance(), tickTimeBarPos.sub(0, 0, 0.001));
        createBarEntity(world.instance(), memoryUsageBarPos.sub(0, 0, 0.001));

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

        // Start the task
        server.scheduler().submitTask(this::handleDisplayUpdate, ExecutionType.TICK_START);
    }

    public TaskSchedule handleDisplayUpdate() {
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

    private Block percentToColor(double value) {
        if (value < 0.6) {
            return Block.LIME_CONCRETE;
        } else if (value < 0.8) {
            return Block.YELLOW_CONCRETE;
        } else {
            return Block.RED_CONCRETE;
        }
    }

    private TextDisplayMeta createTextEntity(Instance instance, Pos pos) {
        var entity = new Entity(EntityType.TEXT_DISPLAY) {
            {
                hasPhysics = false;
                setNoGravity(true);
            }

            @Override protected void movementTick() {
                // Intentionally do nothing
            }
        };

        var meta = (TextDisplayMeta) entity.getEntityMeta();
        meta.setBackgroundColor(0);
        meta.setAlignLeft(true);
        meta.setAlignRight(false);
        entity.setInstance(instance, pos);
        return meta;
    }

    private BlockDisplayMeta createBarEntity(Instance instance, Pos pos) {
        var entity = new Entity(EntityType.BLOCK_DISPLAY) {
            {
                hasPhysics = false;
                setNoGravity(true);
            }

            @Override protected void movementTick() {
                // Intentionally do nothing
            }
        };

        var meta = (BlockDisplayMeta) entity.getEntityMeta();
        meta.setBlockState(Block.GRAY_CONCRETE);
        meta.setScale(new Vec(MAX_WIDTH, 0.3, 0.001));
        entity.setInstance(instance, pos);
        return meta;
    }
}
