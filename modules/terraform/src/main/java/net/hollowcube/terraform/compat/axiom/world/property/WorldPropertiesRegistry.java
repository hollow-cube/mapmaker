package net.hollowcube.terraform.compat.axiom.world.property;

import net.hollowcube.terraform.compat.axiom.Axiom;
import net.hollowcube.terraform.compat.axiom.packet.client.AxiomClientSetWorldPropertyPacket;
import net.hollowcube.terraform.compat.axiom.packet.server.AxiomRegisterWorldPropertiesPacket;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public final class WorldPropertiesRegistry {
    private static final Tag<WorldPropertiesRegistry> TAG = Tag.Transient("terraform:axiom/world_properties_registry");

    public static @NotNull WorldPropertiesRegistry get(@NotNull Instance instance) {
        var registry = instance.getTag(TAG);
        if (registry == null) {
            registry = new WorldPropertiesRegistry(instance);
            instance.setTag(TAG, registry);
        }
        return registry;
    }

    private final Instance instance;

    private final Map<Category, List<WorldProperty<?>>> categoryMap = new LinkedHashMap<>();
    private final Map<NamespaceID, WorldProperty<?>> idToPropertyMap = new HashMap<>();

    public WorldPropertiesRegistry(@NotNull Instance instance) {
        this.instance = instance;

        var eventNode = EventNode.type("terraform:axiom/world_properties", EventFilter.INSTANCE);
        eventNode.setPriority(-10000000);
        eventNode.addListener(AddEntityToInstanceEvent.class, this::handleEntityAddedToInstance);
        eventNode.addListener(RemoveEntityFromInstanceEvent.class, this::handleEntityRemovedFromInstance);
        instance.eventNode().addChild(eventNode);
    }

    public void add(@NotNull Category category) {
        if (categoryMap.containsKey(category)) return;
        categoryMap.put(category, new ArrayList<>());
    }

    public void add(@NotNull Category category, @NotNull WorldProperty<?> property) {
        Check.argCondition(idToPropertyMap.containsKey(property.id()), "Property with ID " + property.id() + " already exists");

        categoryMap.computeIfAbsent(category, c -> new ArrayList<>()).add(property);
        idToPropertyMap.put(property.id(), property);

        updateRegistration();
    }

    public void add(@NotNull Category category, @NotNull WorldProperty<?>... properties) {
        for (var property : properties)
            Check.argCondition(idToPropertyMap.containsKey(property.id()), "Property with ID " + property.id() + " already exists");

        categoryMap.computeIfAbsent(category, c -> new ArrayList<>()).addAll(Arrays.asList(properties));
        for (var property : properties)
            idToPropertyMap.put(property.id(), property);

        updateRegistration();
    }

    public void remove(@NotNull NamespaceID id) {
        if (idToPropertyMap.remove(id) == null) return;
        for (var properties : categoryMap.values()) {
            var removed = properties.removeIf(property -> property.id().equals(id));
            if (removed) break;
        }

        updateRegistration();
    }

    public boolean handlePropertyChange(@NotNull Player player, @NotNull AxiomClientSetWorldPropertyPacket packet) {
        //noinspection unchecked
        var property = (WorldPropertyImpl<Object>) idToPropertyMap.get(packet.id());
        if (property == null) return false;

        var dataType = property.type().dataType();
        if (dataType.typeId() != packet.typeId()) return false;

        property.update(player, dataType.deserialize(packet.value()));
        return true;
    }

    private void handleEntityAddedToInstance(@NotNull AddEntityToInstanceEvent event) {
        if (!(event.getEntity() instanceof Player player) || !Axiom.isPresent(player)) return;
        player.sendPacket(createRegistrationPacket(player));
    }

    private void handleEntityRemovedFromInstance(@NotNull RemoveEntityFromInstanceEvent event) {
        if (!(event.getEntity() instanceof Player player) || !Axiom.isPresent(player)) return;

        //todo need to inform the properties that the player is no longer in the instance/to toss the value.
    }

    private void updateRegistration() {
        if (instance.getPlayers().isEmpty()) return;

        for (var player : instance.getPlayers()) {
            if (!Axiom.isPresent(player)) continue;
            player.sendPacket(createRegistrationPacket(player));
        }
    }

    private @NotNull ServerPacket createRegistrationPacket(@NotNull Player player) {
        // Most of this doesn't change much, but some could be per player so a bit hard to cache

        //todo one idea for caching is the following:
        //- make WorldProperty viewable
        //- always cache the registration packet with the default value
        //- when a player joins, send the cached packet
        //- when added as a viewer, send the current value from inside the WorldProperty itself

        var packetProperties = new HashMap<AxiomRegisterWorldPropertiesPacket.Category, List<AxiomRegisterWorldPropertiesPacket.WorldProperty>>();
        for (var entry : categoryMap.entrySet()) {
            var category = entry.getKey();
            var properties = entry.getValue();

            if (properties.isEmpty()) continue; // Do not send empty categories

            var packetPropertiesList = new ArrayList<AxiomRegisterWorldPropertiesPacket.WorldProperty>();
            for (var property : properties) {
                //noinspection unchecked
                DataType<Object> dataType = (DataType<Object>) property.type().dataType();
                var packetProperty = new AxiomRegisterWorldPropertiesPacket.WorldProperty(
                        property.id(),
                        property.name(),
                        property.localizeName(),
                        property.type().typeId(),
                        property.type().properties(),
                        dataType.serialize(property.getValue(player))
                );
                packetPropertiesList.add(packetProperty);
            }

            var packetCategory = new AxiomRegisterWorldPropertiesPacket.Category(category.name(), category.localizeName());
            packetProperties.put(packetCategory, packetPropertiesList);
        }

        return Axiom.writePacket(new AxiomRegisterWorldPropertiesPacket(packetProperties));
    }
}
