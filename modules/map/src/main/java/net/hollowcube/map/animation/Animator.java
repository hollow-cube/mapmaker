package net.hollowcube.map.animation;

/**
 * An animator is responsible for animating a single object as part of a larger animation.
 *
 * <p>An animator instance is created for each entity, so it is valid to keep state.</p>
 */
public interface Animator {

    /**
     * Set the animation to the given tick, resetting all state to that point.
     */
    void seek(int tick);

    /**
     * Begin playback from the current position (can be set with {@link #seek(int)}).
     */
    void play();

    /**
     * Pause playback at the current position.
     */
    void pause();

    void tick();

}
