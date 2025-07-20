package net.hollowcube.mapmaker.map.block.custom;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandler;
import net.hollowcube.mapmaker.map.util.RelativePos;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class HappyGhastMarkerHandler extends ObjectEntityHandler {
    public static final String ID = "mapmaker:happy_ghast";

    public record Data(
            @NotNull List<RelativePos> path,
            double speed,
            boolean smooth,
            boolean loop,
            @Nullable NamedTextColor harness,
            double scale
    ) {
        private static final double DEFAULT_SPEED = 3.6; // blocks per second
        public static final StructCodec<Data> CODEC = StructCodec.struct(
                // While technically these can contain pitch and yaw, it is always ignored.
                "path", RelativePos.ARRAY_CODEC.list(64).optional(List.of()), Data::path,
                "speed", Codec.DOUBLE.optional(DEFAULT_SPEED), Data::speed,
                "smooth", Codec.BOOLEAN.optional(false), Data::smooth,
                "loop", Codec.BOOLEAN.optional(false), Data::loop,
                "harness", ExtraCodecs.NAMED_TEXT_COLOR.optional(), Data::harness,
                "scale", ExtraCodecs.clamppedDouble(0.0625, 1.0).optional(1.0), Data::scale,
                Data::new);
    }

    private record Keyframe(double time, Vec position) {
    }

    private final LivingEntity ghastEntity;

    private List<Keyframe> keyframes;
    private double totalDuration = 0;
    private boolean smooth;
    private long startTick;

    public HappyGhastMarkerHandler(@NotNull MarkerEntity entity) {
        super(ID, entity);
        this.ghastEntity = new LivingEntity(EntityType.HAPPY_GHAST) {{
            setNoGravity(true);
            hasPhysics = false;
        }};
        var spawnPosition = entity.getPosition().sub(0, ghastEntity.getBoundingBox().height() / 2, 0);
        ghastEntity.setInstance(entity.getInstance(), spawnPosition);
        onDataChange(null); // init
    }

    @Override
    public void onPositionChange(@NotNull Pos newPosition) {
        ghastEntity.teleport(newPosition.sub(0, ghastEntity.getBoundingBox().height() / 2, 0));
        onDataChange(null); // Need to re-resolve relative positions
    }

    @Override
    public void onDataChange(@Nullable Player player) {
        var parseResult = entity.getData(Data.CODEC);
        if (!(parseResult instanceof Result.Ok(var data))) {
            if (player != null) {
                var msg = ((Result.Error<?>) parseResult).message();
                player.sendMessage("Invalid marker data: " + msg);
            }
            return;
        }


        double lastTime = 0;
        var lastPos = entity.getPosition().withView(0, 0);
        var newKeyframes = new ArrayList<Keyframe>(data.path().size());
        newKeyframes.add(new Keyframe(0, Vec.fromPoint(lastPos)));
        for (int i = 0; i < data.path.size(); i++) {
            var relativePos = data.path().get(i);
            var position = relativePos.resolve(entity.getPosition())
                    .withView(0, 0);
            var duration = lastPos.distance(position) / Math.abs(data.speed);
            lastTime += duration;
            lastPos = position;
            newKeyframes.add(new Keyframe(lastTime, Vec.fromPoint(position)));
        }
        if (data.loop && newKeyframes.size() > 1) {
            // If looping, add the start to the end
            var endPos = newKeyframes.get(newKeyframes.size() - 1).position;
            var startPos = newKeyframes.getFirst().position;
            var duration = endPos.distance(startPos) / Math.abs(data.speed);
            lastTime += duration;
            newKeyframes.add(new Keyframe(lastTime, startPos));
        }
        this.keyframes = newKeyframes;
        this.totalDuration = lastTime;
        this.smooth = data.smooth;
        this.startTick = ghastEntity.getAliveTicks();

        ghastEntity.getAttribute(Attribute.SCALE).setBaseValue(data.scale);
        if (data.harness != null) {
            var harnessMaterial = Material.fromKey("minecraft:" + data.harness + "_harness");
            ghastEntity.setEquipment(EquipmentSlot.BODY, ItemStack.of(harnessMaterial));
        }
    }

    @Override
    public void onTick() {
        if (keyframes == null || keyframes.isEmpty()) return;

        double timeSeconds = (ghastEntity.getAliveTicks() - startTick) / 20.0;
        timeSeconds %= totalDuration; // Looping

        // Find the current keyframe
        int currentIndex = 0;
        for (int i = 0; i < keyframes.size(); i++) {
            if (keyframes.get(i).time > timeSeconds) {
                currentIndex = i - 1;
                break;
            }
        }
        int nextIndex = Math.min(currentIndex + 1, keyframes.size() - 1);
        if (currentIndex < 0) currentIndex = 0;
        if (nextIndex < 0) nextIndex = 0;

        var currentKeyframe = keyframes.get(currentIndex);
        var nextKeyframe = keyframes.get(nextIndex);
        double t = (timeSeconds - currentKeyframe.time) / (nextKeyframe.time - currentKeyframe.time);
        var interpolatedPos = smooth
                ? interpolateCatmullrom(currentIndex, nextIndex, t)
                : interpolateLinear(currentIndex, nextIndex, t);

        // Update the ghast entity's position
        ghastEntity.teleport(new Pos(interpolatedPos));
    }

    @Override
    public void onRemove() {
        ghastEntity.remove();
    }

    private Vec interpolateLinear(int currentIndex, int nextIndex, double t) {
        var current = keyframes.get(currentIndex).position;
        var next = keyframes.get(nextIndex).position;
        return current.lerp(next, t);
    }

    private Vec interpolateCatmullrom(int currentIndex, int nextIndex, double t) {
        var lastLast = keyframes.get(Math.max(0, currentIndex - 1)).position;
        var last = keyframes.get(currentIndex).position;
        var next = keyframes.get(nextIndex).position;
        var nextNext = keyframes.get(Math.min(keyframes.size() - 1, nextIndex + 1)).position;
        return new Vec(
                catmullrom(t, lastLast.x(), last.x(), next.x(), nextNext.x()),
                catmullrom(t, lastLast.y(), last.y(), next.y(), nextNext.y()),
                catmullrom(t, lastLast.z(), last.z(), next.z(), nextNext.z())
        );
    }

    private static double catmullrom(double f, double g, double h, double i, double j) {
        return 0.5F * (2.0F * h + (i - g) * f + (2.0F * g - 5.0F * h + 4.0F * i - j) * f * f + (3.0F * h - g - 3.0F * i + j) * f * f * f);
    }

}
