package net.hollowcube.map.animation;

import net.hollowcube.map.animation.animator.AnimatorV2;
import net.hollowcube.map.animation.property.Properties;
import net.hollowcube.map.animation.property.Property;
import net.hollowcube.map.world.EditingMapWorld;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.FontUIBuilder;
import net.hollowcube.terraform.event.TerraformModifyEntityEvent;
import net.hollowcube.terraform.event.TerraformMoveEntityEvent;
import net.hollowcube.terraform.event.TerraformSpawnEntityEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.particle.ParticleCreator;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AnimationBuilder {
    private final EditingMapWorld world;
    private final EventNode<InstanceEvent> eventNode;
    private Audience audience = Audience.empty();

    private String name = null;
    private List<AnimatorV2> animators = new ArrayList<>();

    private int tick = 0;

    private Task task = null;

    public AnimationBuilder(@NotNull EditingMapWorld world) {
        this.world = world;
        this.eventNode = EventNode.type("animation_builder", EventFilter.INSTANCE, this::isEventRelevant)
                .addListener(TerraformSpawnEntityEvent.class, this::handleEntitySpawn)
                .addListener(TerraformMoveEntityEvent.class, this::handleEntityMove)
                .addListener(TerraformModifyEntityEvent.class, this::handleEntityModified);
        world.addScopedEventNode(eventNode);
    }

    public void seek(int tick) {
        var wasPlaying = task != null;
        if (wasPlaying) {
            pause();
            world.instance().scheduleNextTick(instance -> {
                seek(tick);
                play();

            });
            return;
        }


        this.tick = tick;
        animators.forEach(animator -> {
            animator.seek(tick);
            animator.sync();
        });
    }

    public void play() {
        play(1);
    }

    public void play(int delay) {
        if (task != null) {
            throw new IllegalStateException("Animation is already playing");
        }

        this.task = world.instance().scheduler().scheduleTask(this::tick, TaskSchedule.tick(delay), TaskSchedule.tick(1));
    }

    public void pause() {
        if (task == null) {
            throw new IllegalStateException("Animation is not playing");
        }

        task.cancel();
        task = null;

        seek(tick);
    }

    public void step() {
        seek(tick + 1);
    }

    private void tick() {
        tick++;
        animators.forEach(Animator::tick);
    }

    public void begin(@NotNull String name) {
        this.name = name;
//        this.currentFrame = new KeyFrame();
//        frames.add(currentFrame);

        for (var player : world.players()) {
            var actionbar = ActionBar.forPlayer(player);
            actionbar.addProvider(new ActionBarProvider());
        }

        this.audience = world.instance();
    }

    public boolean isActive() {
        return name != null;
    }

    private boolean isEventRelevant(@NotNull InstanceEvent event, @NotNull Instance instance) {
        return isActive() && world.instance().equals(instance);
    }

    private void handleEntitySpawn(@NotNull TerraformSpawnEntityEvent event) {
        var animator = new AnimatorV2(world.instance(), event.getEntity(), tick);
        event.setEntity(animator.getEntity());
        this.animators.add(animator);

        // Add an initial keyframe with the position
        animator.keyframe(Properties.POSITION).setValue(event.getPosition());
        buildParticleData();

        audience.sendMessage(Component.text("Added entity " + event.getEntity().getUuid()));
    }

    private void handleEntityMove(@NotNull TerraformMoveEntityEvent event) {
        var animator = getAnimatorForEntity(event.getEntity());
        if (animator == null) return;

        animator.keyframe(Properties.POSITION).setValue(event.getNewPosition());
        buildParticleData();
        System.out.println("MOVED ENTITY " + event.getEntity().getUuid() + " TO " + event.getNewPosition() + " AT " + tick);
    }

    private void handleEntityModified(@NotNull TerraformModifyEntityEvent event) {
        var animator = getAnimatorForEntity(event.getEntity());
        if (animator == null) return;

        var entityMeta = event.getEntity().getEntityMeta();
        if (entityMeta instanceof AbstractDisplayMeta meta) {
            assignIfChanged(animator, Properties.SCALE, meta.getScale());
        }
    }

    private <T> void assignIfChanged(@NotNull AnimatorV2 animator, @NotNull Property<T> property, @NotNull T newValue) {
        boolean isDefaultValue = property.defaultValue().equals(newValue);

        // Never assign if the property is equal to the default value and the animator doesn't have the property already
        if (isDefaultValue && !animator.hasProperty(property))
            return;

        // Get the closest keyframe to check if the value is already set
        var closest = animator.keyframe(property, false);
        if (newValue.equals(closest.value())) return;

        // The value seems to have changed, so we should assign it
        animator.keyframe(property).setValue(newValue);
        System.out.println(property + "=" + newValue + " at t=" + tick);
        buildParticleData();
    }

    private @Nullable AnimatorV2 getAnimatorForEntity(@NotNull Entity entity) {
        for (var animator : animators) {
            if (animator.getEntity().equals(entity)) {
                return animator;
            }
        }
        return null;
    }

    private List<ParticlePacket> particles = List.of();

    private void buildActionBar(@NotNull Player player, @NotNull FontUIBuilder builder) {
        builder.append("Current Tick: " + tick);

        particles.forEach(player::sendPacket);
    }

    public void sendDebugInfo(@NotNull Player player) {
        if (!isActive()) {
            player.sendMessage("Animation builder is not active");
            return;
        }

        var builder = Component.text("name: " + name).appendNewline()
                .append(Component.text("tick: " + tick)).appendNewline()
                .append(Component.text("animators: " + animators.size())).appendNewline();

        for (var animator : animators) {
            builder = builder.append(Component.text("  - " + animator.getEntity().getUuid())).appendNewline();
            for (var entry : animator.getKeyframes().entrySet()) {
                builder = builder.append(Component.text("  " + entry.getKey())).appendNewline();
                for (var keyframe : entry.getValue().keyframes) {
                    builder = builder.append(Component.text("    - " + keyframe.time() + ": " + keyframe.value())).appendNewline();
                }
            }
        }

        player.sendMessage(builder);

        buildParticleData();

//        player.sendMessage("current: " + currentFrame);
    }

    private void buildParticleData() {
        var points = new ArrayList<Point>();

        for (var animator : animators) {
            var positionFrames = animator.getKeyframes().get(Properties.POSITION);
            if (positionFrames == null) continue;

            for (var keyframe : positionFrames.keyframes) {
                points.add((Pos) keyframe.value());
            }
        }

        var particles = new ArrayList<ParticlePacket>();
        for (int i = 0; i < points.size() - 1; i++) {
            var from = points.get(i);
            var to = points.get(i + 1);

            particles.addAll(createParticleLine(from, to, 0.1f, 0xFF0000));
        }

        this.particles = particles;
    }

    private @NotNull List<ParticlePacket> createParticleLine(@NotNull Point from, @NotNull Point to, float size, int color) {
        var particles = new ArrayList<ParticlePacket>();

        var distance = from.distance(to);
        var direction = Vec.fromPoint(to.sub(from)).normalize();
        var step = 0.25f;

        for (float i = 0; i < distance; i += step) {
            var point = from.add(direction.mul(i));
            particles.add(ParticleCreator.createParticlePacket(
                    Particle.DUST, true,
                    point.x(), point.y(), point.z(),
                    0, 0, 0,
                    0, 1, buffer -> {
                        buffer.writeFloat(255f / 255f);
                        buffer.writeFloat(1f / 255f);
                        buffer.writeFloat(1f / 255f);
                        buffer.writeFloat(0.5f);
                    }
            ));
        }

        return particles;
    }


    private class ActionBarProvider implements ActionBar.Provider {
        @Override
        public void provide(@NotNull Player player, @NotNull FontUIBuilder builder) {
            buildActionBar(player, builder);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ActionBarProvider;
        }

        @Override
        public int hashCode() {
            return ActionBarProvider.class.hashCode();
        }
    }
}
