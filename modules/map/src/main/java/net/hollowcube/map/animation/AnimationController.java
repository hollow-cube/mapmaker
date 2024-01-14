package net.hollowcube.map.animation;

import net.minestom.server.instance.Instance;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AnimationController implements Animator {

    private final Instance instance;
    private final List<? extends Animator> animation;

    private Task task = null;

    public AnimationController(@NotNull Instance instance, @NotNull List<? extends Animator> animation) {
        this.instance = instance;
        this.animation = animation;
    }

    public @NotNull Instance instance() {
        return instance;
    }

    @Override
    public void seek(int tick) {
        animation.forEach(animator -> animator.seek(tick));
    }

    @Override
    public void play() {
        if (task != null) {
            throw new IllegalStateException("Animation is already playing");
        }

        this.task = instance.scheduler().scheduleTask(this::tick, TaskSchedule.tick(1), TaskSchedule.tick(1));
        animation.forEach(Animator::play);
    }

    @Override
    public void pause() {
        if (task == null) {
            throw new IllegalStateException("Animation is not playing");
        }

        task.cancel();
    }

    @Override
    public void tick() {
        animation.forEach(Animator::tick);
    }
}
