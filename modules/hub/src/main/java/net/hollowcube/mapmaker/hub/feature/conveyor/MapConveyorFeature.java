package net.hollowcube.mapmaker.hub.feature.conveyor;

import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import net.hollowcube.common.math.Quaternion;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.anim.AnimationEntity;
import net.hollowcube.mapmaker.hub.anim.Channel;
import net.hollowcube.mapmaker.hub.anim.ChannelImpl;
import net.hollowcube.mapmaker.hub.anim.Keyframe;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.color.Color;
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
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@AutoService(HubFeature.class)
public class MapConveyorFeature implements HubFeature {

    private static final List<BadSprite> SPRITES = List.of(
            BadSprite.require("5x5/amethyst_factory_kowji"),
            BadSprite.require("5x5/blossom_itmg"),
            BadSprite.require("5x5/mini_factory_tado"),
            BadSprite.require("5x5/prairie_settlement_sethprg"),
            BadSprite.require("5x5/ravine_rankup_kowji"),
            BadSprite.require("5x5/small_house"),
            BadSprite.require("5x5/stylized_floating_parkour_ossipago1")
    );

    private static final double SCALE_FACTOR = 4.5;

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

    private static int distance(int blocks) {
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

    private int spawnIndex = 0;

    private final List<Entity> theEntities = new ArrayList<>();

    @Inject
    public MapConveyorFeature(@NotNull HubMapWorld world, @NotNull Scheduler scheduler) {

        scheduler.submitTask(() -> {
            if (spawnIndex++ % 2 == 0) {
                spawnLeftComplexMap(world);
            } else {
                spawnLeftSimpleMap(world);
            }

            spawnCenterMap(world);

            return TaskSchedule.tick((int) (7.5 * 20));
        });

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
    }

    private @NotNull AnimationEntity initMapEntity() {
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
        entity.onReset = () -> ((ItemDisplayMeta) entity.getEntityMeta()).setItemStack(getRandomMapItem());
        var meta = entity.getEntityMeta();
        meta.setScale(new Vec(4.5)); // 5x5
        //todo setting a width and height makes them all lag behind in very unpredictable ways.
        // No idea why, feels like a vanilla bug but i assume im just misunderstanding something
//        meta.setWidth(5);
//        meta.setHeight(5);
        ((ItemDisplayMeta) meta).setDisplayContext(ItemDisplayMeta.DisplayContext.THIRD_PERSON_RIGHT_HAND);

        // Random 0,90,180,270
        // Random -5,5 degrees added
        var rand = ThreadLocalRandom.current();
        var rotation = (rand.nextInt(4) * 90) + rand.nextInt(-5, 6);
        entity.getEntityMeta().setLeftRotation(new Quaternion(new Vec(0, 1, 0), Math.toRadians(rotation)).into());
        return entity;
    }

    private void spawnLeftComplexMap(@NotNull HubMapWorld world) {
        var entity = initMapEntity();
        entity.setKeyframes(LEFT_COMPLEX_MAP);

        var spawnPos = ((ChannelImpl.Position.Value) LEFT_COMPLEX_MAP.get(0).getOrDefault(Channel.POSITION)).vec();
        entity.setInstance(world.instance(), spawnPos).thenRun(entity::spawnHitbox);
    }

    private void spawnLeftSimpleMap(@NotNull HubMapWorld world) {
        var entity = initMapEntity();
        entity.setKeyframes(LEFT_SIMPLE);

        var spawnPos = ((ChannelImpl.Position.Value) LEFT_SIMPLE.get(0).getOrDefault(Channel.POSITION)).vec();
        entity.setInstance(world.instance(), spawnPos).thenRun(entity::spawnHitbox);
    }

    private void spawnCenterMap(@NotNull HubMapWorld world) {
        var entity = initMapEntity();
        entity.setKeyframes(CENTER);

        var spawnPos = ((ChannelImpl.Position.Value) CENTER.get(0).getOrDefault(Channel.POSITION)).vec();
        entity.setInstance(world.instance(), spawnPos).thenRun(entity::spawnHitbox);
    }

    private @NotNull ItemStack getRandomMapItem() {
        var sprite = SPRITES.get((int) (ThreadLocalRandom.current().nextDouble() * SPRITES.size()));
        return ItemStack.of(Material.LEATHER_HORSE_ARMOR)
                .with(ItemComponent.CUSTOM_MODEL_DATA, sprite.cmd())
                .with(ItemComponent.DYED_COLOR, new DyedItemColor(new Color(0x77c652)));
    }


}
