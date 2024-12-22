package net.hollowcube.mapmaker.hub.feature.conveyor;

import net.hollowcube.common.math.Quaternion;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.anim.AnimationEntity;
import net.hollowcube.mapmaker.hub.anim.Channel;
import net.hollowcube.mapmaker.hub.anim.ChannelImpl;
import net.hollowcube.mapmaker.hub.anim.Keyframe;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.color.Color;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.DyedItemColor;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

//@AutoService(HubFeature.class)
public class MapConveyorFeature implements HubFeature {

    private static final BadSprite PISTON_SPRITE = BadSprite.require("hub/extra/big_piston");
    private static final List<BadSprite> MAP_SPRITES = List.of(
            BadSprite.require("hub/5x5/amethyst_factory_kowji"),
            BadSprite.require("hub/5x5/blossom_itmg"),
            BadSprite.require("hub/5x5/mini_factory_tado"),
//            BadSprite.require("hub/5x5/prairie_settlement_sethprg"),
            BadSprite.require("hub/5x5/ravine_rankup_kowji"),
            BadSprite.require("hub/5x5/small_house"),
            BadSprite.require("hub/5x5/stylized_floating_parkour_ossipago1")
    );
    private static final List<BadSprite> MATERIAL_SPRITES = List.of(
            BadSprite.require("hub/materials/0"),
            BadSprite.require("hub/materials/1"),
            BadSprite.require("hub/materials/2"),
            BadSprite.require("hub/materials/3"),
            BadSprite.require("hub/materials/4"),
            BadSprite.require("hub/materials/5"),
            BadSprite.require("hub/materials/6"),
            BadSprite.require("hub/materials/7"),
            BadSprite.require("hub/materials/8"),
            BadSprite.require("hub/materials/13"),
            BadSprite.require("hub/materials/14"),
            BadSprite.require("hub/materials/15"),
            BadSprite.require("hub/materials/16"),
            BadSprite.require("hub/materials/17"),
            BadSprite.require("hub/materials/blob")
    );
    private static final BadSprite BLOB_SPRITE = BadSprite.require("hub/materials/blob");

    // 0.15 blocks per tick
//    private static final double BLOCKS_PER_TICK = 0.5;
    private static final double BLOCKS_PER_TICK = 0.09;

    private static int tempT = 0;

    private static int start() {
        tempT = 0;
        return 0;
    }

    private static int sameTick() {
        return wait(2) | Keyframe.NO_INTERP;
//        return tempT;
    }

    private static int distance(double blocks) {
        tempT += (int) (blocks / BLOCKS_PER_TICK);
        return tempT;
    }

    private static int rotation(int degrees, int radius) {
        var arcLength = (2 * Math.PI * radius) * (degrees / 360.);
        tempT += (int) ((arcLength / BLOCKS_PER_TICK) * 0.25); // 0.25 speeds it up arbitrarily.
        return tempT;
    }

    private static int wait(int ticks) {
        tempT += ticks;
        return tempT;
    }

    private static void gravity(@NotNull List<Keyframe> keyframes, @NotNull Pos start, @NotNull Pos end) {
        // 9.8 meters per second == 9.8 blocks per second accel
        // except apparently my math is bad so i just divided by 4 to make it feel nicer :)
        double accel = 9.8 / 20 / 4; // Accel per tick
        int steps = 3; // divide into 3 interp steps (since interpolation is linear)
        var direction = Vec.fromPoint(end.sub(start));
        double totalDistance = direction.length();
        int totalTime = (int) Math.ceil(Math.sqrt(2 * totalDistance / accel));
        int stepTime = totalTime / steps;

        var dir = direction.normalize();

        for (int i = 0; i < steps - 1; i++) {
            double t = (i + 1) * stepTime;
            double distanceCovered = 0.5 * accel * t * t;
            var keyframePosition = start.add(dir.mul(distanceCovered));
            keyframes.add(new Keyframe(tempT + (i + 1) * stepTime, Channel.POSITION.set(keyframePosition)));
        }

        // Ensure final step is always at the end point exactly
        keyframes.add(new Keyframe(tempT + totalTime, Channel.POSITION.set(end)));

        tempT += totalTime;
    }

//    private static

    private static @NotNull List<Keyframe> matchLength(@NotNull List<Keyframe> keyframes, @NotNull List<Keyframe> target) {
        var targetLength = target.get(target.size() - 1).t();
        var keyframesLength = keyframes.get(keyframes.size() - 1).t();
        var diff = targetLength - keyframesLength;
        if (diff == 0) return keyframes;

        var newKeyframes = new ArrayList<>(keyframes);
        newKeyframes.add(new Keyframe(keyframesLength + diff));
        return newKeyframes;
    }

    private final AnimationEntity clawPillar = new AnimationEntity(EntityType.ITEM_DISPLAY, false);
    private final AnimationEntity clawClaw = new AnimationEntity(EntityType.ITEM_DISPLAY, false);

    private final AnimationEntity sealPiston = new AnimationEntity(EntityType.ITEM_DISPLAY, false);

    private final List<Keyframe> LEFT_COMPLEX_PILLAR = List.of(
            new Keyframe(start()),
            new Keyframe(distance(4)),
            new Keyframe(distance(4), Channel.POSITION.set(new Pos(-64.5, 37, 38.5, 180, 0))),
            new Keyframe(sameTick()),

            new Keyframe(rotation(90, 7), Channel.POSITION.set(new Pos(-64.5, 37, 38.5, 270, 0))),
            new Keyframe(sameTick()),

            new Keyframe(distance(4)),
            new Keyframe(wait(10)),

            new Keyframe(distance(4), Channel.POSITION.set(new Pos(-64.5, 37, 38.5, 270, 0))),
            new Keyframe(rotation(90, 7), Channel.POSITION.set(new Pos(-64.5, 37, 38.5, 180, 0)))
    );
    private final List<Keyframe> LEFT_COMPLEX_CLAW = List.of(
            new Keyframe(start()),
            new Keyframe(distance(4), Channel.POSITION.set(new Pos(-64.5, 37, 38.5, 180, 0))),
            new Keyframe(distance(4), Channel.POSITION.set(new Pos(-64.5, 42, 38.5, 180, 0))),
            new Keyframe(sameTick()),

            new Keyframe(rotation(90, 7), Channel.POSITION.set(new Pos(-64.5, 42, 38.5, 270, 0))),
            new Keyframe(sameTick()),

            new Keyframe(distance(4), Channel.POSITION.set(new Pos(-64.5, 37, 38.5, 270, 0))), // Down
            new Keyframe(wait(10)),
            new Keyframe(distance(4), Channel.POSITION.set(new Pos(-64.5, 42, 38.5, 270, 0))), // up
            new Keyframe(rotation(90, 7), Channel.POSITION.set(new Pos(-64.5, 42, 38.5, 180, 0)))
    );

    private final List<Keyframe> LEFT_COMPLEX_MAP = List.of(
            new Keyframe(start(), Channel.POSITION.set(new Pos(-71.5, 37, 85.5))),
            new Keyframe(distance(85 - 38), () -> {
                clawPillar.playOnce(LEFT_COMPLEX_PILLAR);
                clawClaw.playOnce(LEFT_COMPLEX_CLAW);
            }, Channel.POSITION.set(new Pos(-71.5, 37, 38.5))),
            new Keyframe(wait(1)),
            new Keyframe(distance(4)), // Wait for claw

            new Keyframe(distance(4), Channel.POSITION.set(new Pos(-71.5, 42, 38.5))), // Move up

            new Keyframe(sameTick(), Channel.POSITION.set(new Pos(-64.5, 42, 38.5)), // Shift pivot
                    Channel.TRANSLATION.set(new Vec(-7, 0, 0))),

            new Keyframe(rotation(90, 7), Channel.POSITION.set(new Pos(-64.5, 42, 38.5, 90, 0))), // Rotate

            new Keyframe(sameTick(), Channel.POSITION.set(new Pos(-64.5, 42, 31.5, 90, 0)), // Shift pivot
                    Channel.TRANSLATION.defaultValue()),
            new Keyframe(distance(4), Channel.POSITION.set(new Pos(-64.5, 37, 31.5, 90, 0))), // Move down
            new Keyframe(distance(31 - 22), Channel.POSITION.set(new Pos(-64.5, 37, 22.5, 90, 0))),
            new Keyframe(distance(81 - 64), Channel.POSITION.set(new Pos(-81.5, 37, 22.5, 90, 0))),
            new Keyframe(distance(22 - 4), Channel.POSITION.set(new Pos(-81.5, 37, 4.5, 90, 0)))
    );
    private final List<Keyframe> LEFT_SIMPLE = matchLength(List.of(
            new Keyframe(start(), Channel.POSITION.set(new Pos(-71.5, 37, 85.5))),
            new Keyframe(distance(85 - 32), Channel.POSITION.set(new Pos(-71.5, 37, 32.5))),
            new Keyframe(distance(91 - 71), Channel.POSITION.set(new Pos(-91.5, 37, 32.5))),
            new Keyframe(distance(32 - 4), Channel.POSITION.set(new Pos(-91.5, 37, 4.5)))
    ), LEFT_COMPLEX_MAP);

    private final List<Keyframe> CENTER = List.of(
            new Keyframe(start(), Channel.POSITION.set(new Pos(-57.5, 34, 53.5))),
            new Keyframe(distance((int) (53.5 + 65.5)), Channel.POSITION.set(new Pos(-57.5, 34, -65.5)))
    );

    private HubMapWorld world;

    private final List<Keyframe> SEAL_PISTON = List.of(
            new Keyframe(start(), Channel.POSITION.set(new Pos(-57.5, 40 + 2.5, -85))),
            new Keyframe(wait(3), () -> {
                world.instance().playSound(Sound.sound(SoundEvent.BLOCK_PISTON_EXTEND, Sound.Source.BLOCK, 0.5f, ThreadLocalRandom.current().nextFloat() * 0.25f + 0.6f), new Pos(-57.5, 40 + 2.5, -85));
            }, Channel.POSITION.set(new Pos(-57.5, 40 + 2.5, -85 - 6))),
            new Keyframe(wait(10)),
            new Keyframe(wait(3), () -> {
                world.instance().playSound(Sound.sound(SoundEvent.BLOCK_PISTON_CONTRACT, Sound.Source.BLOCK, 0.5f, ThreadLocalRandom.current().nextFloat() * 0.25f + 0.6f), new Pos(-57.5, 40 + 2.5, -85));
            }, Channel.POSITION.set(new Pos(-57.5, 40 + 2.5, -85))),
            new Keyframe(wait(10))
    );

    private final List<Keyframe> RIGHT_MATERIAL_STANDARD = List.of(
            new Keyframe(start(), Channel.POSITION.set(new Pos(8.5, 40, -35.5))),
            new Keyframe(distance((int) (8.5 + 32.5)), Channel.POSITION.set(new Pos(-32.5, 40, -35.5))),
            new Keyframe(distance(87 - 35), Channel.POSITION.set(new Pos(-32.5, 40, -87.5))),
            new Keyframe(distance(67 - 32), Channel.POSITION.set(new Pos(-67.5, 40, -87.5))),
            new Keyframe(distance(87 - 65), Channel.POSITION.set(new Pos(-67.5, 40, -65.5)))
    );

    private void startLaserLine(@NotNull List<ServerPacket> particles) {
        int time = (int) (5 / BLOCKS_PER_TICK);
        final AtomicInteger remaining = new AtomicInteger(time / 5);
        world.instance().scheduler().submitTask(() -> {
            if (remaining.decrementAndGet() < 0)
                return TaskSchedule.stop();

            particles.forEach(world.instance()::sendGroupedPacket);
            return TaskSchedule.tick(5);
        });
    }

    private @NotNull List<Keyframe> makeRightSealPath() {
        var path = new ArrayList<Keyframe>();

        path.add(new Keyframe(start(), Channel.POSITION.set(new Pos(8.5, 40, -35.5))));
        path.add(new Keyframe(distance((int) (8.5 + 32.5)), Channel.POSITION.set(new Pos(-32.5, 40, -35.5))));
        path.add(new Keyframe(distance(87 - 35), Channel.POSITION.set(new Pos(-32.5, 40, -87.5))));

        path.add(new Keyframe(distance(35 - 32.5), () -> {
            startLaserLine(FIRST_LASER_LINE);
        }, Channel.POSITION.set(new Pos(-35, 40, -87.5)))); // Start laser 1
        path.add(new Keyframe(distance(5), () -> {
            startLaserLine(SECOND_LASER_LINE);
        }, Channel.POSITION.set(new Pos(-40, 40, -87.5)))); // Start laser 2
        path.add(new Keyframe(distance(5), () -> {
            startLaserLine(THIRD_LASER_LINE);
        }, Channel.POSITION.set(new Pos(-45, 40, -87.5)))); // Start laser 3
        path.add(new Keyframe(distance(5), () -> {
            startLaserLine(FOURTH_LASER_LINE);
        }, Channel.POSITION.set(new Pos(-50, 40, -87.5)))); // Start laser 4

        path.add(new Keyframe(distance(57 - 50.5), Channel.POSITION.set(new Pos(-57.5, 40, -87.5))));
        path.add(new Keyframe(wait(1), () -> {
            sealPiston.playOnce(SEAL_PISTON);
        }));
        path.add(new Keyframe(wait(3), Channel.POSITION.set(new Pos(-57.5, 40, -93.5))));

        gravity(path, new Pos(-57.5, 40, -93.5), new Pos(-57.5, 18, -93.5));

        for (var p : path) {
            System.out.println(p);
        }

        return path;
    }

    private final List<Keyframe> RIGHT_MATERIAL_SEAL = makeRightSealPath();

    private static @NotNull List<ServerPacket> createParticleLine(@NotNull Point from, @NotNull Point to, int color, float size) {
        var length = from.distance(to);
        var direction = Vec.fromPoint(to.sub(from)).normalize();

        var particles = new ArrayList<ServerPacket>();
        for (double i = 0; i < length; i += 0.25) {
            var pos = from.add(direction.mul(i));
            particles.add(new ParticlePacket(Particle.DUST.withProperties(TextColor.color(color), size), (float) pos.x(), (float) pos.y(), (float) pos.z(), 0, 0, 0, 0f, 1));
        }

        return particles;
    }

    private static <T> @NotNull List<T> merge(@NotNull List... packets) {
        var merged = new ArrayList();
        for (var packet : packets) {
            merged.addAll(packet);
        }
        return merged;
    }

    private final List<ServerPacket> FIRST_LASER_LINE = merge(
            createParticleLine(new Vec(-37.5, 40.5, -85), new Vec(-37.5, 43.5, -90), 0xFF0000, 1f),
            createParticleLine(new Vec(-37.5, 40.5, -90), new Vec(-37.5, 43.5, -85), 0xFF0000, 1f)
    );

    private final List<ServerPacket> SECOND_LASER_LINE = merge(
            createParticleLine(new Vec(-42.5, 40.5, -85), new Vec(-42.5, 43.5, -90), 0xFF0000, 1f),
            createParticleLine(new Vec(-42.5, 40.5, -90), new Vec(-42.5, 43.5, -85), 0xFF0000, 1f)
    );

    private final List<ServerPacket> THIRD_LASER_LINE = merge(
            createParticleLine(new Vec(-47.5, 40.5, -85), new Vec(-47.5, 43.5, -90), 0xFF0000, 1f),
            createParticleLine(new Vec(-47.5, 40.5, -90), new Vec(-47.5, 43.5, -85), 0xFF0000, 1f)
    );

    private final List<ServerPacket> FOURTH_LASER_LINE = merge(
            createParticleLine(new Vec(-52.5, 40.5, -85), new Vec(-52.5, 43.5, -90), 0xFF0000, 1f),
            createParticleLine(new Vec(-52.5, 40.5, -90), new Vec(-52.5, 43.5, -85), 0xFF0000, 1f)
    );

    private int spawnIndex = 0;

    private final List<Entity> theEntities = new ArrayList<>();

    @Override
    public void load(@NotNull MapServer server, @NotNull HubMapWorld world) {
        this.world = world;
        server.scheduler().submitTask(() -> {
            if (spawnIndex++ % 2 == 0) {
                spawnEntity(world, LEFT_COMPLEX_MAP, MAP_SPRITES, true);
            } else {
                spawnEntity(world, LEFT_SIMPLE, MAP_SPRITES, true);
            }

            spawnEntity(world, CENTER, MAP_SPRITES, true);

            var materialSprite = BLOB_SPRITE;
//            var materialSprite = randomSprite(MATERIAL_SPRITES);
            var keyframes = RIGHT_MATERIAL_STANDARD;
            if (materialSprite == BLOB_SPRITE) keyframes = RIGHT_MATERIAL_SEAL;
            spawnEntity(world, keyframes, materialSprite, false);

            return TaskSchedule.tick((int) (7.5 * 20));
        });

//        scheduler.submitTask(() -> {
//
//            FIRST_LASER_LINE.forEach(world.instance()::sendGroupedPacket);
//            SECOND_LASER_LINE.forEach(world.instance()::sendGroupedPacket);
//            THIRD_LASER_LINE.forEach(world.instance()::sendGroupedPacket);
//            FOURTH_LASER_LINE.forEach(world.instance()::sendGroupedPacket);
//
//            return TaskSchedule.tick(5);
//        });

        world.instance().eventNode().addListener(AddEntityToInstanceEvent.class, event -> {
            if (!(event.getEntity() instanceof Player player)) return;
            theEntities.forEach(e -> e.addViewer(player));
        });

        ((ItemDisplayMeta) clawPillar.getEntityMeta()).setItemStack(ItemStack.of(Material.STICK).with(ItemComponent.CUSTOM_MODEL_DATA, 17));
        clawPillar.getEntityMeta().setScale(new Vec(10));
        clawPillar.getInterp().setTranslation(new Vec(-2 / 4., 20 / 4., 0));
        clawPillar.setInstance(world.instance());
        clawPillar.getInterp().setPosition(new Pos(-64.5, 37, 38.5, 180, 0));
        theEntities.add(clawPillar);

        ((ItemDisplayMeta) clawClaw.getEntityMeta()).setItemStack(ItemStack.of(Material.STICK).with(ItemComponent.CUSTOM_MODEL_DATA, 16));
        clawClaw.getEntityMeta().setScale(new Vec(9));
        clawClaw.getInterp().setTranslation(new Vec(8 / 4., 12 / 4., 0));
        clawClaw.setInstance(world.instance());
        clawClaw.getInterp().setPosition(new Pos(-64.5, 42, 38.5, 180, 0));
        theEntities.add(clawClaw);

        ((ItemDisplayMeta) sealPiston.getEntityMeta()).setItemStack(ItemStack.of(Material.STICK).with(ItemComponent.CUSTOM_MODEL_DATA, 16));
        var sealMeta = (ItemDisplayMeta) sealPiston.getEntityMeta();
        sealMeta.setDisplayContext(ItemDisplayMeta.DisplayContext.THIRD_PERSON_RIGHT_HAND);
        sealMeta.setItemStack(makeItemStack(PISTON_SPRITE));
        sealMeta.setScale(new Vec(10)); // 10 is the biggest dimension
        sealMeta.setLeftRotation(new Quaternion(new Vec(1, 0, 0), Math.toRadians(90)).into());
        sealPiston.setInstance(world.instance());
        sealPiston.getInterp().setPosition(new Pos(-57.5, 40 + 2.5, -85, 0, 0));

        theEntities.add(sealPiston);
    }

    private @NotNull AnimationEntity initMapEntity(@NotNull BadSprite sprite) {
        var entity = new AnimationEntity(EntityType.ITEM_DISPLAY, true) {
            @Override
            public void spawn() {
                super.spawn();
                theEntities.add(this);
                getInstance().getPlayers().forEach(this::addViewer);
            }

            @Override
            protected void remove(boolean permanent) {
                super.remove(permanent);
                theEntities.remove(this);
            }
        };
        entity.setAutoViewable(false);

        // Set the sprite
        var itemMeta = (ItemDisplayMeta) entity.getEntityMeta();
        itemMeta.setDisplayContext(ItemDisplayMeta.DisplayContext.THIRD_PERSON_RIGHT_HAND);
        itemMeta.setItemStack(makeItemStack(sprite));
        itemMeta.setScale(new Vec(sprite.width() * 0.9)); // Would turn 5x5x5 into 4.5 scale for example

        //todo setting a width and height makes them all lag behind in very unpredictable ways.
        // No idea why, feels like a vanilla bug but i assume im just misunderstanding something
//        meta.setWidth(5);
//        meta.setHeight(5);

        // Random 0,90,180,270
        // Random -5,5 degrees added
        var rand = ThreadLocalRandom.current();
        var rotation = (rand.nextInt(4) * 90) + rand.nextInt(-5, 6);
        entity.getEntityMeta().setLeftRotation(new Quaternion(new Vec(0, 1, 0), Math.toRadians(rotation)).into());
        return entity;
    }

    private @NotNull BadSprite randomSprite(@NotNull List<BadSprite> spriteSet) {
        return spriteSet.get((int) (ThreadLocalRandom.current().nextDouble() * spriteSet.size()));
    }

    private void spawnEntity(@NotNull HubMapWorld world, @NotNull List<Keyframe> keyframes, @NotNull List<BadSprite> spriteSet, boolean needsHitbox) {
        spawnEntity(world, keyframes, randomSprite(spriteSet), needsHitbox);
    }

    private void spawnEntity(@NotNull HubMapWorld world, @NotNull List<Keyframe> keyframes, @NotNull BadSprite sprite, boolean needsHitbox) {
        var entity = initMapEntity(sprite);
        entity.setKeyframes(keyframes);
        var spawnPos = ((ChannelImpl.Position.Value) keyframes.get(0).getOrDefault(Channel.POSITION)).vec();
        entity.setInstance(world.instance(), spawnPos).thenRun(() -> {
            if (needsHitbox) entity.spawnHitbox();
        });
    }

    private @NotNull ItemStack makeItemStack(@NotNull BadSprite sprite) {
        return ItemStack.of(Material.LEATHER_HORSE_ARMOR)
                .with(ItemComponent.CUSTOM_MODEL_DATA, sprite.cmd())
                .with(ItemComponent.DYED_COLOR, new DyedItemColor(new Color(0x77c652)));
    }

}
