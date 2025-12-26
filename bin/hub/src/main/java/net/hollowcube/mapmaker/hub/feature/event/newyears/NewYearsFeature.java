package net.hollowcube.mapmaker.hub.feature.event.newyears;

import com.google.auto.service.AutoService;
import net.hollowcube.common.util.ColorUtil;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.map.MapServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.HoverEvent;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.component.FireworkExplosion;
import net.minestom.server.item.component.FireworkList;
import net.minestom.server.timer.TaskSchedule;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRulesException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@AutoService(HubFeature.class)
public class NewYearsFeature implements HubFeature {

    private static final Map<ZoneId, List<TimezoneRegion>> ZONES_TO_REGIONS = new HashMap<>();

    static {
        try (var is = NewYearsFeature.class.getResourceAsStream("/timezones.txt")) {
            var content = new String(Objects.requireNonNull(is).readAllBytes());
            for (var line : content.split("\n")) {
                var parts = line.split("\\|");
                var country = parts[0];
                var region = parts[1];
                try {
                    var zoneId = ZoneId.of(parts[2]);
                    ZONES_TO_REGIONS.computeIfAbsent(zoneId, _ -> new ArrayList<>())
                        .add(new TimezoneRegion(country, region));
                } catch (ZoneRulesException e) {
                    // Invalid timezone, skip
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Vec MIN_SPAWN_BOX = new Vec(-215, 95, -65);
    private static final Vec MAX_SPAWN_BOX = new Vec(-175, 96, 65);
    private static final FireworkExplosion.Shape[] FIREWORK_SHAPES = FireworkExplosion.Shape.values();
    private static final int FIREWORKS_PER_CELEBRATION = 25;
    private static final int FIREWORKS_SPAWN_DELAY_MIN = 0;
    private static final int FIREWORKS_SPAWN_DELAY_MAX = 20;
    private static final int FIREWORK_TICKS_MIN = 15;
    private static final int FIREWORK_TICKS_MAX = 25;

    @Override
    public void load(MapServer server, HubMapWorld world) {
        var timeUntilNext15MinuteMark = 900 - (System.currentTimeMillis() / 1000 % 900);
        server.scheduler()
            .buildTask(() -> {
                Map<String, List<String>> celebrating = new HashMap<>();

                for (var entry : ZONES_TO_REGIONS.entrySet()) {
                    if (isNewYearsInZone(entry.getKey())) {
                        for (var region : entry.getValue()) {
                            celebrating.computeIfAbsent(region.country, _ -> new ArrayList<>()).add(region.region);
                        }
                    }
                }

                if (!celebrating.isEmpty()) {
                    sendMessage(world, celebrating);

                    var random = ThreadLocalRandom.current();
                    for (int i = 0; i < FIREWORKS_PER_CELEBRATION; i++) {
                        var position = new Vec(
                            random.nextDouble(MIN_SPAWN_BOX.x(), MAX_SPAWN_BOX.x()),
                            random.nextDouble(MIN_SPAWN_BOX.y(), MAX_SPAWN_BOX.y()),
                            random.nextDouble(MIN_SPAWN_BOX.z(), MAX_SPAWN_BOX.z())
                        );
                        var delay = random.nextInt(FIREWORKS_SPAWN_DELAY_MIN, FIREWORKS_SPAWN_DELAY_MAX + 1);
                        if (delay > 0) {
                            server.scheduler()
                                .buildTask(() -> spawnFirework(world, position))
                                .delay(TaskSchedule.tick(delay))
                                .schedule();
                        } else {
                            spawnFirework(world, position);
                        }
                    }

                    server.scheduler()
                        .buildTask(() -> {
                            new CustomFirework(20, FireworkShapes.TWO)
                                .withRotation(-90)
                                .setInstance(world.instance(), new Vec(-200, 95, 4));
                            new CustomFirework(20, FireworkShapes.ZERO)
                                .withRotation(-90)
                                .setInstance(world.instance(), new Vec(-200, 95, 1.5));
                            new CustomFirework(20, FireworkShapes.TWO)
                                .withRotation(-90)
                                .setInstance(world.instance(), new Vec(-200, 95, -1.5));
                            new CustomFirework(20, FireworkShapes.SIX)
                                .withRotation(-90)
                                .setInstance(world.instance(), new Vec(-200, 95, -4));
                        })
                        .delay(TaskSchedule.tick(FIREWORKS_SPAWN_DELAY_MAX + 1))
                        .schedule();
                }
            })
            .delay(TaskSchedule.seconds(timeUntilNext15MinuteMark))
            .repeat(TaskSchedule.minutes(15))
            .schedule();
    }

    private static void spawnFirework(HubMapWorld world, Vec position) {
        var instance = world.instance();
        var random = ThreadLocalRandom.current();

        var fireworks = new FireworkList(0, List.of(new FireworkExplosion(
            FIREWORK_SHAPES[random.nextInt(FIREWORK_SHAPES.length)],
            List.of(ColorUtil.fromHsv(random.nextFloat(), 1f, 1f)),
            List.of(ColorUtil.fromHsv(random.nextFloat(), 1f, 1f)),
            true,
            true
        )));
        var ticks = random.nextInt(FIREWORK_TICKS_MIN, FIREWORK_TICKS_MAX + 1);
        var firework = new Firework(ticks, fireworks);
        firework.setInstance(instance, position);
    }

    private static void sendMessage(HubMapWorld world, Map<String, List<String>> countries) {
        var places = new ArrayList<Component>();

        for (var entry : countries.entrySet()) {
            var country = entry.getKey();
            var regionList = entry.getValue();

            if (regionList.size() == 1 && regionList.getFirst().isEmpty()) {
                places.add(Component.text(country));
            } else if (regionList.size() == 1) {
                places.add(Component.text("%s (%s)".formatted(regionList.getFirst(), country)));
            } else {
                var lore = Component.text(country)
                    .appendNewline()
                    .append(regionList.stream()
                                .map(region -> Component.text(" - " + region))
                                .collect(Component.toComponent(Component.text("\n")))
                    );
                places.add(Component.text("*" + country).hoverEvent(HoverEvent.showText(lore)));
            }
        }

        world.instance().sendMessage(Component.translatable(
            "new_years.message",
            Component.join(JoinConfiguration.commas(true), places)
        ));
    }

    private static boolean isNewYearsInZone(ZoneId zoneId) {
        var now = ZonedDateTime.now(zoneId);
        if (now.getMonthValue() == 12 && now.getDayOfMonth() == 31 && now.getHour() == 23 && now.getMinute() >= 55) {
            return true;
        } else if (now.getMonthValue() == 1 && now.getDayOfMonth() == 1 && now.getHour() == 0 && now.getMinute() < 5) {
            return true;
        }
        return false;
    }

    private record TimezoneRegion(String country, String region) {}
}
