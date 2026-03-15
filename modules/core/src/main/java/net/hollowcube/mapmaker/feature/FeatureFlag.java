package net.hollowcube.mapmaker.feature;

import org.jetbrains.annotations.NonBlocking;

/**
 * A feature flag is a boolean value which can be enabled or disabled at runtime,
 * and which can differ based on some context provided such as player id or map id.
 *
 * <p>Because feature flags may be changed at runtime, it is <i>never</i> acceptable
 * to cache a returned true or false value. {@link #test(Object...)} will never block
 * so it is safe to call at any point.</p>
 */
public interface FeatureFlag {

    static FeatureFlag of(String name) {
        return new BasicFeatureFlag(name);
    }

    static FeatureFlag never() {
        return ignored -> false;
    }

    /**
     * Tests whether the feature is enabled with the given context.
     *
     * <p>This function will never block the current thread. If the feature flag data is
     * for some reason not loaded from an external source, it will return false with no
     * other indication.</p>
     *
     * <p>This function will never throw an exception. If an exception does occur it will
     * be logged to the {@link net.minestom.server.exception.ExceptionHandler} and false
     * will be returned with no other indication.</p>
     *
     * @param context Any context required to test the flag, such as a player or map id.
     * @return True if the feature is known to be enabled, false otherwise.
     */
    @NonBlocking
    boolean test(Object... context);

}
