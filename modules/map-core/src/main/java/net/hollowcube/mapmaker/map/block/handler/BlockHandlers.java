package net.hollowcube.mapmaker.map.block.handler;

import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.block.BlockHandler;

import java.util.function.Consumer;

public final class BlockHandlers {

    // List from https://minecraft.wiki/w/Block_entity
    public static final BlockHandler SIGN = new SignBlockHandler("minecraft:sign");
    public static final BlockHandler HANGING_SIGN = new SignBlockHandler("minecraft:hanging_sign");
    public static final BlockHandler BANNER = new BannerBlockHandler();
    public static final BlockHandler CHEST = new NoopBlockHandler("minecraft:chest");
    public static final BlockHandler TRAPPED_CHEST = new NoopBlockHandler("minecraft:trapped_chest");
    public static final BlockHandler SHULKER_BOX = new NoopBlockHandler("minecraft:shulker_box");
    public static final BlockHandler MONSTER_SPAWNER = new MobSpawnerBlockHandler();
    public static final BlockHandler END_PORTAL = new NoopBlockHandler("minecraft:end_portal");
    public static final BlockHandler ENDER_CHEST = new NoopBlockHandler("minecraft:ender_chest");
    public static final BlockHandler MOB_HEAD = new NoopBlockHandler("minecraft:skull");
    public static final BlockHandler PLAYER_HEAD = new PlayerHeadBlockHandler();
    public static final BlockHandler BED = new NoopBlockHandler("minecraft:bed");
    public static final BlockHandler CONDUIT = new NoopBlockHandler("minecraft:conduit");
    public static final BlockHandler BELL = new NoopBlockHandler("minecraft:bell");
    public static final BlockHandler ENCHANTING_TABLE = new NoopBlockHandler("minecraft:enchanting_table");
    public static final BlockHandler DECORATED_POT = new DecoratedPotBlockHandler();
    public static final BlockHandler CAMPFIRE = new CampfireBlockHandler();
    public static final BlockHandler STRUCTURE = new StructureBlockHandler();
    public static final BlockHandler SHELF = new ShelfBlockHandler();

    public static void init() {
        Consumer<BlockHandler> register = handler -> MinecraftServer.getBlockManager()
                .registerHandler(handler.getKey(), () -> handler);

        register.accept(SIGN);
        register.accept(HANGING_SIGN);
        register.accept(BANNER);
        register.accept(CHEST);
        register.accept(TRAPPED_CHEST);
        register.accept(SHULKER_BOX);
        register.accept(MONSTER_SPAWNER);
        register.accept(END_PORTAL);
        register.accept(ENDER_CHEST);
        register.accept(MOB_HEAD);
        register.accept(PLAYER_HEAD);
        register.accept(BED);
        register.accept(CONDUIT);
        register.accept(BELL);
        register.accept(ENCHANTING_TABLE);
        register.accept(DECORATED_POT);
        register.accept(CAMPFIRE);
        register.accept(STRUCTURE);
        register.accept(SHELF);
    }
}
