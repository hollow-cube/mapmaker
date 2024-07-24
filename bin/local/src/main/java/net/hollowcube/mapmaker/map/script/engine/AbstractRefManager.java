package net.hollowcube.mapmaker.map.script.engine;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.func.LuaFunctions;
import net.hollowcube.luau.util.Pin;
import net.minestom.server.entity.Entity;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * Manages references to resources created by a script so that script objects may remain as small stateless wrappers.
 */
public abstract class AbstractRefManager {
    private final EventNode<InstanceEvent> parentNode;

    // Ephemeral resources
    private final List<Pin<?>> pins = new ArrayList<>();
    private final EventNode<InstanceEvent> eventNode;
    private final List<Entity> entities = new ArrayList<>();

    protected AbstractRefManager(@NotNull EventNode<InstanceEvent> parentNode, @NotNull Predicate<InstanceEvent> eventFilter) {
        this.parentNode = parentNode;
        this.eventNode = EventNode.event("script-local-" + ThreadLocalRandom.current().nextInt(0, 9999), EventFilter.INSTANCE, eventFilter);
        parentNode.addChild(eventNode);
    }

    public <F> @NotNull F bindFunction(Class<F> functionType, @NotNull LuaState state, int index) {
        var funcPin = LuaFunctions.bind(functionType, state, index);
        pins.add(funcPin);
        return funcPin.get();
    }

    public void addListener(@NotNull EventListener<?> listener) {
        //noinspection unchecked
        eventNode.addListener((EventListener<? extends InstanceEvent>) listener);
    }

    public void addEntity(@NotNull Entity entity) {
        entities.add(entity);
    }

    public void close() {
        parentNode.removeChild(eventNode);

        for (Entity entity : entities) entity.remove();
        entities.clear();

        for (Pin<?> pin : pins) pin.close();
        pins.clear();
    }
}
