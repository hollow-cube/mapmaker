package net.hollowcube.compat.noxesium.components;

import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.Unit;

import java.util.List;

public class NoxesiumGameComponents {

    // other components exist, but we don't define them as we don't use them

    public static final NoxesiumComponentRegistry REGISTRY = new NoxesiumComponentRegistry("game_components");

    public static NoxesiumComponentType<Unit> CAMERA_LOCKED = REGISTRY.register("camera_locked", NetworkBuffer.UNIT);
    public static NoxesiumComponentType<Unit> DISABLE_BOAT_COLLISIONS = REGISTRY.register("disable_boat_collisions", NetworkBuffer.UNIT);

    public static NoxesiumComponentType<Unit> DISABLE_SPIN_ATTACK_COLLISIONS = REGISTRY.register("disable_spin_attack_collisions", NetworkBuffer.UNIT);

    public static NoxesiumComponentType<Unit> CLIENT_AUTHORITATIVE_ELYTRA = REGISTRY.register("client_authoritative_elytra", NetworkBuffer.UNIT);
    public static NoxesiumComponentType<Double> ELYTRA_COYOTE_TIME = REGISTRY.register("elytra_coyote_time", NetworkBuffer.DOUBLE);

    public static NoxesiumComponentType<Integer> HELD_ITEM_NAME_OFFSET = REGISTRY.register("held_item_name_offset", NetworkBuffer.VAR_INT);
    public static NoxesiumComponentType<List<ItemStack>> CUSTOM_CREATIVE_ITEMS = REGISTRY.register("custom_creative_items", ItemStack.STRICT_NETWORK_TYPE.list());
}
