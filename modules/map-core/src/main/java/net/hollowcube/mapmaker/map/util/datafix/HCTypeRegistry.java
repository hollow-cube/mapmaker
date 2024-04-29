package net.hollowcube.mapmaker.map.util.datafix;

import ca.spottedleaf.dataconverter.minecraft.datatypes.IDDataType;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCDataType;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import net.hollowcube.mapmaker.map.util.datafix.versions.DataFix;

import java.util.ServiceLoader;

public final class HCTypeRegistry {

    // Entities are stored as NBT using the vanilla Entity format with a handful of extensions for extra data.
    // Extensions are registered on the original MCTypeRegistry.ENTITY (and entities should be converted as such)

    public static final MCDataType WORLD = new MCDataType("HC/World"); // Polar world user data
    public static final MCDataType CHUNK = new MCDataType("HC/Chunk"); // Polar chunk user data

    public static final IDDataType ENTITIY = MCTypeRegistry.ENTITY;
    public static final IDDataType BLOCK_ENTITY = MCTypeRegistry.TILE_ENTITY;

    static {
        try {
            Class.forName(MCTypeRegistry.class.getName()); // Make sure MCTypeRegistry has definitely done static init
            registerAll();
        } catch (final Throwable e) {
            throw new RuntimeException("failed to register hc fixers", e);
        }
    }

    private static void registerAll() {
        ServiceLoader.load(DataFix.class).forEach(DataFix::register);
    }

    private HCTypeRegistry() {
    }
}
