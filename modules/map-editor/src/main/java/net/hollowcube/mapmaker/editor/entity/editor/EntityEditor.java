package net.hollowcube.mapmaker.editor.entity.editor;

import net.hollowcube.common.util.PlayerUtil;
import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.mapmaker.map.entity.MapEntityType;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoRegistry;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerPickEntityEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.TypedCustomData;
import net.minestom.server.registry.RegistryKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EntityEditor {

    private static final Map<EntityType, Material> ENTITY_TO_ITEM = new ConcurrentHashMap<>();
    static {
        for (var value : Material.values()) {
            var data = value.prototype().get(DataComponents.ENTITY_DATA);
            if (data == null) continue;
            ENTITY_TO_ITEM.put(data.type(), value);
        }
        ENTITY_TO_ITEM.put(EntityType.ARMOR_STAND, Material.ARMOR_STAND);
        ENTITY_TO_ITEM.put(EntityType.END_CRYSTAL, Material.END_CRYSTAL);
    }

    public static void handlePickEntity(PlayerPickEntityEvent event) {
        var player = event.getPlayer();
        var world = EditorMapWorld.forPlayer(player);
        if (world == null) return;
        if (!(event.getTarget() instanceof MapEntity<?> entity)) return;
        if (!MapEntityType.hasOverride(entity.getEntityType())) return;
        if (!world.canEdit(player)) return;

        var item = ENTITY_TO_ITEM.get(entity.getEntityType());
        if (item == null) return;
        var stack = ItemStack.builder(item);
        if (event.isIncludeData()) {
            var data = CompoundBinaryTag.builder();
            entity.writeData(data);

            stack.set(DataComponents.ENTITY_DATA, new TypedCustomData<>(entity.getEntityType(), data.build()));
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

            var info = MapEntityInfoRegistry.get(entity);
            if (info != null) {
                var properties = new HashMap<Component, Component>();
                for (var property : info.properties()) {
                    properties.put(Component.text(property.name(), NamedTextColor.WHITE), getPropertyDisplay(property, entity));
                }

                var lore = new ArrayList<Component>();
                lore.add(Component.empty());
                for (var entry : properties.entrySet()) {
                    lore.add(Component.empty().decoration(TextDecoration.ITALIC, false)
                                 .append(entry.getKey().append(Component.text(": ")).append(entry.getValue())));
                }
                stack.set(DataComponents.LORE, lore);
            }
        }

        PlayerUtil.giveItem(player, stack.build());
    }

    public static void handleItemUse(PlayerBlockInteractEvent event) {
        var player = event.getPlayer();
        var item = player.getItemInHand(event.getHand());
        var world = EditorMapWorld.forPlayer(player);
        var data = item.get(DataComponents.ENTITY_DATA);
        if (world == null) return;
        if (data == null) return;
        if (!MapEntityType.hasOverride(data.type())) return;

        var position = event.getBlockPosition().relative(event.getBlockFace());
        var entity = MapEntityType.create(data.type(), UUID.randomUUID());
        entity.setInstance(world.instance(), position.add(0.5, 0, 0.5));

        if (entity instanceof MapEntity<?> me && !data.nbt().isEmpty()) {
            me.readData(data.nbt());
        }

        event.setBlockingItemUse(true);
        event.setCancelled(true);
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends MapEntity<?>> Component getPropertyDisplay(MapEntityInfo.Property<T, E> property, MapEntity<?> entity) {
        var value = property.type().get((E) entity);
        return switch (value) {
            case Enum<?> e -> Component.text(e.name(), NamedTextColor.AQUA);
            case Boolean b -> b ? Component.text("True", NamedTextColor.GREEN) : Component.text("False", NamedTextColor.RED);
            case Byte b -> Component.text(b, NamedTextColor.YELLOW);
            case Short s -> Component.text(s, NamedTextColor.YELLOW);
            case Integer i -> Component.text(i, NamedTextColor.YELLOW);
            case Long l -> Component.text(l, NamedTextColor.YELLOW);
            case Float f -> Component.text(f, NamedTextColor.YELLOW);
            case Double d -> Component.text(d, NamedTextColor.YELLOW);
            case String s -> Component.text("\"" + s + "\"", NamedTextColor.DARK_GREEN);
            case RegistryKey<?> rk -> Component.text(rk.name(), NamedTextColor.BLUE);
            default -> Component.text(value.toString(), NamedTextColor.GRAY);
        };
    }
}
