package net.hollowcube.compat.noxesium.qib;

import com.noxcrew.noxesium.api.qib.QibDefinition;
import net.hollowcube.compat.noxesium.packets.ClientboundChangeServerRulesPacket;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class QibDefinitionManager {

    public static final Tag<Map<String, QibDefinition>> QIB_DEFINITIONS = Tag.Transient("noxesium:qib/definitions");

    private static void updateDefinitions(Instance instance, Consumer<Map<String, QibDefinition>> consumer) {
        Map<String, QibDefinition> defs = Objects.requireNonNullElseGet(instance.getTag(QIB_DEFINITIONS), ConcurrentHashMap::new);
        consumer.accept(defs);
        instance.setTag(QIB_DEFINITIONS, defs);
        ClientboundChangeServerRulesPacket.qibs(defs).sendToViewers(instance.getPlayers());
    }

    public static void set(Instance instance, String id, QibDefinition definition) {
        updateDefinitions(instance, defs -> defs.put(id, definition));
    }

    public static void remove(Instance instance, String id) {
        updateDefinitions(instance, defs -> defs.remove(id));
    }
}
