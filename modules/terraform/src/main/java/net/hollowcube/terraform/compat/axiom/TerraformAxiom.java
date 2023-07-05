package net.hollowcube.terraform.compat.axiom;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.hollowcube.terraform.give_me_new_home.PaletteUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.play.AcknowledgeBlockChangePacket;
import net.minestom.server.network.packet.server.play.MultiBlockChangePacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

import static net.minestom.server.network.NetworkBuffer.*;

@SuppressWarnings("UnstableApiUsage")
public class TerraformAxiom {
    private static final System.Logger logger = System.getLogger(TerraformAxiom.class.getName());

    private static final int MAX_SECTIONS_PER_UPDATE = Integer.getInteger("terraform.axiom.max_sections_per_update", 1024);

    private static final int EMPTY_BLOCK_STATE = Block.STRUCTURE_VOID.stateId();

    public static void init(@NotNull EventNode<? extends InstanceEvent> eventNode, @Nullable CommandCondition condition) {
        MinecraftServer.getGlobalEventHandler()
                .addListener(PlayerPluginMessageEvent.class, TerraformAxiom::onPluginMessage);
    }

    private static void onPluginMessage(@NotNull PlayerPluginMessageEvent event) {
        var player = event.getPlayer();
        var buffer = new NetworkBuffer(ByteBuffer.wrap(event.getMessage()));
        switch (event.getIdentifier()) {
            case "minecraft:register" -> handleRegisterPluginMessageChannels(player, event.getMessageString());
            case "axiom:set_gamemode" -> handleSetGamemode(player, buffer);
            case "axiom:set_fly_speed" -> handleSetFlySpeed(player, buffer);
            case "axiom:set_block" -> handleSetBlock(player, buffer);
            case "axiom:set_hotbar_slot" -> handleSetHotbarSlot(player, buffer);
            case "axiom:switch_active_hotbar" -> handleSwitchActiveHotbar(player, buffer);
            case "axiom:teleport" -> handleTeleport(player, buffer);
            case "axiom:set_block_buffer" -> handleBigPayload(player, buffer);
            default ->
                    System.out.println("RECEIVED MESSAGE " + event.getIdentifier() + " WITH " + event.getMessageString());
        }
    }

    private static void handleRegisterPluginMessageChannels(@NotNull Player player, @NotNull String data) {
        for (var channel : data.split("\0")) {
            switch (channel) {
                case "axiom:enable" -> handleEnable(player);
                case "axiom:initialize_hotbars" -> handleInitHotbars(player);
                default -> {
                    if (channel.startsWith("axiom:")) {
                        logger.log(System.Logger.Level.WARNING, "Unhandled axiom channel: " + channel);
                    }
                }
            }
        }
    }

    private static void handleEnable(@NotNull Player player) {
        logger.log(System.Logger.Level.INFO, "Axiom is present for " + player.getUsername());
        var data = NetworkBuffer.makeArray(buffer -> {
            buffer.write(BOOLEAN, true);
            buffer.write(BYTE, (byte) 0); // todo: world properties
        });
        player.sendPluginMessage("axiom:enable", data);
    }

    private static void handleInitHotbars(@NotNull Player player) {
        //todo we do not send anything, hotbars are not saved yet.

        //                PersistentDataContainer container = player.getPersistentDataContainer();
        //                int activeHotbarIndex = container.getOrDefault(ACTIVE_HOTBAR_INDEX, PersistentDataType.BYTE, (byte) 0);
        //                PersistentDataContainer hotbarItems = container.get(HOTBAR_DATA, PersistentDataType.TAG_CONTAINER);
        //                if (hotbarItems != null) {
        //                    FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.buffer());
        //                    friendlyByteBuf.writeByte((byte) activeHotbarIndex);
        //                    for (int i=0; i<9*9; i++) {
        //                        // Ignore selected hotbar
        //                        if (i / 9 == activeHotbarIndex) continue;
        //
        //                        ItemStack stack = hotbarItems.get(new NamespacedKey("axiom", "slot_"+i), ItemStackDataType.INSTANCE);
        //                        friendlyByteBuf.writeItem(CraftItemStack.asNMSCopy(stack));
        //                    }
        //                    player.sendPluginMessage(this, "axiom:initialize_hotbars", friendlyByteBuf.array());
        //                }
    }

    private static void handleSetGamemode(@NotNull Player player, @NotNull NetworkBuffer buffer) {
        var gameModeId = buffer.read(BYTE);
        try {
            player.setGameMode(GameMode.fromId(gameModeId));
        } catch (IllegalArgumentException e) {
            logger.log(System.Logger.Level.WARNING, "Invalid gamemode received from {0} ({1})", player.getUuid(), gameModeId);
        }
    }

    private static void handleSetFlySpeed(@NotNull Player player, @NotNull NetworkBuffer buffer) {
        var newFlySpeed = buffer.read(FLOAT);
        player.setFlyingSpeed(newFlySpeed);
    }

    private static void handleSetBlock(@NotNull Player player, @NotNull NetworkBuffer buffer) {
        logger.log(System.Logger.Level.WARNING, "Received axiom:set_block from {0}", player.getUuid());

        var blockPos = buffer.read(BLOCK_POSITION);
        int blockStateId = buffer.read(VAR_INT);
        var block = Block.fromStateId((short) blockStateId);
        var updateNeighbors = buffer.read(BOOLEAN); //todo this doesnt really seem to be supported by minestom, need to add this
        int sequenceId = buffer.read(INT);

        try {
            var instance = player.getInstance();
            if (block == null) {
                logger.log(System.Logger.Level.WARNING, "Invalid block received from {0} ({1})", player.getUuid(), blockStateId);
                return;
            }

            instance.setBlock(blockPos, block);
        } finally {
            player.sendPacket(new AcknowledgeBlockChangePacket(sequenceId));
        }
    }

    private static void handleSetHotbarSlot(@NotNull Player player, @NotNull NetworkBuffer buffer) {
        logger.log(System.Logger.Level.WARNING, "Received axiom:set_hotbar_slot from {0}", player.getUuid());
        //            FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(message));
        //            int index = friendlyByteBuf.readByte();
        //            if (index < 0 || index >= 9*9) return;
        //            net.minecraft.world.item.ItemStack nmsStack = friendlyByteBuf.readItem();
        //
        //            PersistentDataContainer container = player.getPersistentDataContainer();
        //            PersistentDataContainer hotbarItems = container.get(HOTBAR_DATA, PersistentDataType.TAG_CONTAINER);
        //            if (hotbarItems == null) hotbarItems = container.getAdapterContext().newPersistentDataContainer();
        //            hotbarItems.set(new NamespacedKey("axiom", "slot_"+index), ItemStackDataType.INSTANCE, CraftItemStack.asCraftMirror(nmsStack));
        //            container.set(HOTBAR_DATA, PersistentDataType.TAG_CONTAINER, hotbarItems);
    }

    private static void handleSwitchActiveHotbar(@NotNull Player player, @NotNull NetworkBuffer buffer) {
        logger.log(System.Logger.Level.WARNING, "Received axiom:switch_active_hotbar from {0}", player.getUuid());

        //            FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(message));
        //            int oldHotbarIndex = friendlyByteBuf.readByte();
        //            int activeHotbarIndex = friendlyByteBuf.readByte();
        //
        //            ItemStack[] hotbarItems = new ItemStack[9];
        //            for (int i=0; i<9; i++) {
        //                hotbarItems[i] = CraftItemStack.asCraftMirror(friendlyByteBuf.readItem());
        //            }
        //
        //            PersistentDataContainer container = player.getPersistentDataContainer();
        //            PersistentDataContainer containerHotbarItems = container.get(HOTBAR_DATA, PersistentDataType.TAG_CONTAINER);
        //            if (containerHotbarItems == null) containerHotbarItems = container.getAdapterContext().newPersistentDataContainer();
        //
        //            for (int i=0; i<9; i++) {
        //                if (oldHotbarIndex != activeHotbarIndex) {
        //                    int index = oldHotbarIndex*9 + i;
        //                    ItemStack stack = player.getInventory().getItem(i);
        //                    if (stack == null) {
        //                        stack = new ItemStack(Material.AIR);
        //                    } else {
        //                        stack = stack.clone();
        //                    }
        //                    containerHotbarItems.set(new NamespacedKey("axiom", "slot_"+index), ItemStackDataType.INSTANCE, stack);
        //                }
        //                int index = activeHotbarIndex*9 + i;
        //                containerHotbarItems.set(new NamespacedKey("axiom", "slot_"+index), ItemStackDataType.INSTANCE, hotbarItems[i].clone());
        //                if (player.getGameMode() == GameMode.CREATIVE) player.getInventory().setItem(i, hotbarItems[i]);
        //            }
        //
        //            container.set(HOTBAR_DATA, PersistentDataType.TAG_CONTAINER, containerHotbarItems);
        //            container.set(ACTIVE_HOTBAR_INDEX, PersistentDataType.BYTE, (byte) activeHotbarIndex);
    }

    private static void handleTeleport(@NotNull Player player, @NotNull NetworkBuffer buffer) {
        var level = buffer.read(STRING);
        var playerLevelName = player.getInstance().getDimensionType().getName().asString(); //todo minestom does not have a concept of dimension names, it is always set to the dimension type
        if (!level.equals(playerLevelName)) {
            logger.log(System.Logger.Level.WARNING, "Received axiom teleport to different dimension ({0} -> {1}) from {2}",
                    playerLevelName, level, player.getUuid());
            return;
        }

        player.teleport(new Pos(
                buffer.read(DOUBLE),
                buffer.read(DOUBLE),
                buffer.read(DOUBLE),
                buffer.read(FLOAT),
                buffer.read(FLOAT)
        ));
    }

    private static void handleBigPayload(@NotNull Player player, @NotNull NetworkBuffer buffer) {
        var level = buffer.read(STRING);
        var playerLevelName = player.getInstance().getDimensionType().getName().asString(); //todo minestom does not have a concept of dimension names, it is always set to the dimension type
        if (!level.equals(playerLevelName)) {
            logger.log(System.Logger.Level.WARNING, "Received axiom teleport to different dimension ({0} -> {1}) from {2}",
                    playerLevelName, level, player.getUuid());
            return;
        }

        ForkJoinPool.commonPool().submit(() -> asyncApplyBigPayload(player, buffer));
    }

    private static void asyncApplyBigPayload(@NotNull Player player, @NotNull NetworkBuffer buffer) {
        // Reusable buffers
        int[] paletteData = new int[PaletteUtil.BLOCK_PALETTE_SIZE];
        var sectionChangeCache = new LongArrayList(PaletteUtil.BLOCK_PALETTE_SIZE);

        var instance = player.getInstance();
        for (int change = 0; change < MAX_SECTIONS_PER_UPDATE; change++) {
            long index = buffer.read(LONG);
            if (index == Long.MAX_VALUE) return;

            int chunkX = PaletteUtil.getX(index);
            int sectionY = PaletteUtil.getY(index);
            int chunkZ = PaletteUtil.getZ(index);

            // Ensure chunk is loaded
            var chunk = instance.getChunk(chunkX, chunkZ);
            if (chunk == null) {
                logger.log(System.Logger.Level.WARNING, "Received block buffer for unloaded chunk ({0}, {1})",
                        chunkX, chunkZ, player.getUuid());
                continue;
            }

            // Read the palette from the buffer
            byte bits = buffer.read(BYTE);
            switch (bits) {
                case 0 -> { // Vanilla: fixed palette
                    //todo this can be optimized to a single palette replacement
                    Arrays.fill(paletteData, buffer.read(VAR_INT));
                }
                case 1, 2, 3, 4 -> { // Vanilla: Linear palette (always bpe 4)
                    var palette = buffer.readCollection(VAR_INT);

                    long[] data = buffer.read(LONG_ARRAY);
                    PaletteUtil.unpack(paletteData, data, 4);

                    for (int i = 0; i < paletteData.length; i++) {
                        paletteData[i] = palette.get(paletteData[i]);
                    }
                }
                case 5, 6, 7, 8 -> { // Vanilla: Hashmap palette (bpe = bits)
                    var palette = buffer.readCollection(VAR_INT);

                    long[] data = buffer.read(LONG_ARRAY);
                    PaletteUtil.unpack(paletteData, data, bits);

                    for (int i = 0; i < paletteData.length; i++) {
                        paletteData[i] = palette.get(paletteData[i]);
                    }
                }
                default -> { // Vanilla: Global palette (bpe = max))
                    long[] data = buffer.read(LONG_ARRAY);
                    PaletteUtil.unpack(paletteData, data, PaletteUtil.MAX_BITS_PER_ENTRY);
                }
            }

            // Apply the changes to the section and queue the update for the viewers
            var section = chunk.getSection(sectionY);
            sectionChangeCache.clear();
            synchronized (chunk) {
                var indexCache = new AtomicInteger(0);
                section.blockPalette().getAll((sx, sy, sz, stateId) -> {
                    var paletteIndex = indexCache.getAndIncrement();
                    var newBlockState = paletteData[paletteIndex];

                    if (newBlockState == EMPTY_BLOCK_STATE) {
                        paletteData[paletteIndex] = stateId;
                    } else {
                        sectionChangeCache.add(((long) newBlockState << 12) | ((long) sx << 8 | (long) sz << 4 | sy));
                    }
                });

                indexCache.set(0);
                section.blockPalette().setAll((x, y, z) -> paletteData[indexCache.getAndIncrement()]);
            }

            var updateIndex = (((long) chunkX & 0x3FFFFF) << 42) | ((long) sectionY & 0xFFFFF) | (((long) chunkZ & 0x3FFFFF) << 20);
            var packet = new MultiBlockChangePacket(updateIndex, sectionChangeCache.toLongArray());
            chunk.sendPacketsToViewers(packet);
        }

        // If we hit this case, we capped the number of changes allowed to make in a single tick
        // send a log that this happened
        int remaining = 0;
        while (true) {
            long index = buffer.read(LONG);
            if (index == Long.MAX_VALUE) break;
            remaining++;
        }
        logger.log(System.Logger.Level.WARNING, "Received block buffer with too many changes ({0} remaining) from {1}",
                remaining, player.getUuid());
    }
}
