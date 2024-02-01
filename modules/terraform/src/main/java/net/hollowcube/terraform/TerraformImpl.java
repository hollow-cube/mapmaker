package net.hollowcube.terraform;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandManager;
import net.hollowcube.terraform.buffer.BlockBuffer;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.session.PlayerSession;
import net.hollowcube.terraform.session.history.Change;
import net.hollowcube.terraform.storage.TerraformStorage;
import net.hollowcube.terraform.task.Task;
import net.hollowcube.terraform.task.TaskImpl;
import net.hollowcube.terraform.task.TaskResult;
import net.hollowcube.terraform.task.edit.WorldView;
import net.hollowcube.terraform.tool.ToolHandler;
import net.hollowcube.terraform.util.Format;
import net.hollowcube.terraform.util.ThreadUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.ChunkHack;
import net.minestom.server.network.packet.server.play.MultiBlockChangePacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
@ApiStatus.Internal
public final class TerraformImpl implements Terraform {
    private static final Logger logger = LoggerFactory.getLogger(Terraform.class);

    final EventNode<InstanceEvent> eventNode;
    private final TerraformRegistry registry;
    private final TerraformStorage storage;

    private final ToolHandler toolHandler;

    // Tasks
    private final ExecutorService threadPoolCompute;
    private final ExecutorService threadPoolApply;

    TerraformImpl(
            @NotNull Collection<Supplier<TerraformModule>> modules, @NotNull String storage,
            @Nullable EventNode<InstanceEvent> eventNode,
            @NotNull CommandManager commandManager, @Nullable CommandCondition commandCondition
    ) {
        // Set the eventNode or register a new one.
        if (eventNode != null) this.eventNode = eventNode;
        else {
            this.eventNode = EventNode.type("terraform", EventFilter.INSTANCE);
            MinecraftServer.getGlobalEventHandler().addChild(this.eventNode);
        }

        this.toolHandler = new ToolHandler(true);
        this.eventNode.addChild(this.toolHandler.eventNode());

        this.registry = new TerraformRegistry(this, modules, commandManager, commandCondition);

        var storageFactory = Objects.requireNonNull(this.registry.storage(storage), "Storage not found: " + storage);
        this.storage = storageFactory.newStorageFunc().get();

        this.threadPoolCompute = Executors.newFixedThreadPool(1, new ThreadUtil.NamedThreadFactory("tf-compute"));
        this.threadPoolApply = Executors.newFixedThreadPool(1, new ThreadUtil.NamedThreadFactory("tf-apply"));
    }

    @Override
    public @NotNull TerraformRegistry registry() {
        return registry;
    }

    @Override
    public @NotNull TerraformStorage storage() {
        return storage;
    }

    @Override
    public @NotNull ToolHandler toolHandler() {
        return toolHandler;
    }

    // Sessions

    @Override
    public void initPlayerSession(@NotNull Player player, @NotNull String playerId) {
        //todo handle exceptions here i guess
        Check.stateCondition(player.hasTag(PlayerSession.TAG), "Player already has a session");

        var sessionData = storage.loadPlayerSession(playerId);
        var session = new PlayerSession(this, playerId, player, sessionData);
        player.setTag(PlayerSession.TAG, session);
        logger.debug("Created session for {} ({}) withData={}", player.getUuid(), player.getUsername(),
                sessionData != null && sessionData.length > 0);
    }

    @Override
    public void savePlayerSession(@NotNull Player player, boolean drop) {
        var session = PlayerSession.forPlayer(player);
        var sessionData = session.write();
        storage.savePlayerSession(session.id(), sessionData);
        if (drop) player.removeTag(PlayerSession.TAG);
        logger.debug("Saved session for {} ({}) drop={}, size={}", player.getUuid(), player.getUsername(),
                drop, Format.formatBytes(sessionData.length));
    }

    @Override
    public void initLocalSession(@NotNull Player player, @NotNull String sessionId) {
        //todo handle exceptions here i guess
        var instance = Objects.requireNonNull(player.getInstance(), "Player must be in an instance");
        var tag = Tag.<LocalSession>Transient(String.format("terraform:session/%s", player.getUuid()));
        Check.stateCondition(instance.hasTag(tag), "Player already has a local session");

        var playerSession = PlayerSession.forPlayer(player);
        var sessionData = storage.loadLocalSession(playerSession.id(), sessionId);
        var session = new LocalSession(playerSession, sessionId, instance, sessionData);
        instance.setTag(tag, session);
        logger.debug("Created local session for {} ({}) withData={}", player.getUuid(), player.getUsername(),
                sessionData != null && sessionData.length > 0);
    }

    @Override
    public void saveLocalSession(@NotNull Player player, boolean drop) {
        var instance = Objects.requireNonNull(player.getInstance(), "Player must be in an instance");
        var tag = Tag.<LocalSession>Transient(String.format("terraform:session/%s", player.getUuid()));

        var session = Objects.requireNonNull(instance.getTag(tag), "Local session not initialized");
        var sessionData = session.write();
        storage.saveLocalSession(session.playerId(), session.id(), sessionData);
        if (drop) instance.removeTag(tag);
        logger.debug("Saved local session for {} ({}) drop={}, size={}", player.getUuid(), player.getUsername(),
                drop, Format.formatBytes(sessionData.length));
    }


    // Internal Task API

    /**
     * Queues a task to have its compute phase executed, then its apply phase.
     */
    public void submitTask(@NotNull TaskImpl task) {
        logger.debug("{}: submitted", task);
        task.setState(Task.State.QUEUED);

        if (task.computeFunc() != null) {
            threadPoolCompute.submit(computeTask(task));
        } else {
            threadPoolApply.submit(applyTask(task));
        }
    }

    private @NotNull Runnable computeTask(@NotNull TaskImpl task) {
        return () -> {
            try {
                logger.debug("{}: compute started", task);
                task.setState(Task.State.COMPUTE);
                long start = System.nanoTime();

                var world = WorldView.instance(task, task.session().instance());
                var buffer = Objects.requireNonNull(task.computeFunc()).exec(task, world);
                task.setBuffer(buffer);

                logger.debug("{}: compute complete in {}ms ({})", task, (System.nanoTime() - start) / 1_000_000d, Format.formatBytes(buffer.sizeBytes()));
                threadPoolApply.submit(applyTask(task));
                task.setState(Task.State.QUEUED);
            } catch (Throwable t) {
                logger.error("{}: compute failed", task, t);
                task.setState(Task.State.FAILED);
            }
        };
    }

    private @NotNull Runnable applyTask(@NotNull TaskImpl task) {
        Check.notNull(task.buffer(), "Task buffer must not be null when applying");
        return () -> {
            try {
                logger.debug("{}: apply started", task);
                task.setState(Task.State.APPLY);
                long start = System.nanoTime();

                var buffer = task.buffer();
                final var changeCount = new AtomicLong();

                var instance = task.session().instance();

                final var sectionChangeCache = new LongArrayList();
                final var paletteData = new int[4096]; // Reused buffer
                final var indexCache = new AtomicInteger(0);

                final var undoBufferBuilder = BlockBuffer.builder(null); //todo add compute buffer min + max here

                buffer.forEachSection((chunkX, chunkY, chunkZ, palette) -> {
                    var chunk = instance.getChunk(chunkX, chunkZ);
                    if (chunk == null) {
                        // Chunk is not loaded
                        logger.warn("{}: reference to unloaded chunk at {}, {}", task, chunkX, chunkZ);
                        chunk = instance.loadChunk(chunkX, chunkZ).join(); // We are in apply thread, its fine to block
                    }
                    final var chunkRef = chunk; // Final var for lambda

                    sectionChangeCache.clear();

                    var section = chunk.getSection(chunkY);
                    synchronized (chunk) { // Synchronized is OK, we always run this on one of the dedicated threads.
                        //todo optimize palette apply, if the palette is a full chunk of the same block we can do a single fill.
                        //todo replaceall is not working, but that would be ideal. instead we do a get then set.

                        indexCache.set(0);
                        section.blockPalette().getAll((sx, sy, sz, stateId) -> {
                            var paletteIndex = indexCache.getAndIncrement();
                            var newBlockState = palette.get(sx, sy, sz);

                            if (newBlockState == null) {
                                paletteData[paletteIndex] = stateId;
                            } else {
                                paletteData[paletteIndex] = newBlockState.stateId();
                                if (newBlockState.handler() != null || newBlockState.hasNbt()) {
                                    chunkRef.setBlock(chunkX * 16 + sx, chunkY * 16 + sy, chunkZ * 16 + sz, newBlockState);
                                }

                                // Only send a client update if the block was actually modified (and for change counter)
                                if (stateId != newBlockState.stateId()) {
                                    changeCount.incrementAndGet();
                                    sectionChangeCache.add(((long) newBlockState.stateId() << 12) | ((long) sx << 8 | (long) sz << 4 | sy));
                                }

                                // Save old state for undo batch
                                // Note that we save regardless of whether the block was actually modified,
                                // this is because undo-ing should change it in case the block changed after
                                // the apply.
                                //todo we need to save block entities here too
                                undoBufferBuilder.set(
                                        (chunkX << 4) + sx,
                                        (chunkY << 4) + sy,
                                        (chunkZ << 4) + sz,
                                        stateId
                                );
                            }
                        });

                        if (!task.isDryRun()) {
                            indexCache.set(0);
                            section.blockPalette().setAll((sx, sy, sz) -> paletteData[indexCache.getAndIncrement()]);
                        }
                    }

                    if (!task.isDryRun()) {
                        var updateIndex = (((long) chunkX & 0x3FFFFF) << 42) | ((long) chunkY & 0xFFFFF) | (((long) chunkZ & 0x3FFFFF) << 20);
                        var packet = new MultiBlockChangePacket(updateIndex, sectionChangeCache.toLongArray());
                        chunk.sendPacketToViewers(packet); //todo these could be batched perhaps, maybe minestom does it on its own?
                        ChunkHack.invalidateChunk(chunk);
                    }

                    //todo the client is super laggy when sending many of these, perhaps this should be iterated by vertical chunk and resend the entire chunk if there are enough sections changed
                });

                // Append to history
                var undoBuffer = undoBufferBuilder.build();
                if (!task.isDryRun() && !task.isEphemeral()) {
                    task.session().remember(Change.of(undoBuffer, buffer));
                }

                task.setState(Task.State.COMPLETE);
                logger.debug("{}: apply complete in {}ms", task, (System.nanoTime() - start) / 1_000_000d);

                var result = new TaskResult(undoBuffer, buffer, changeCount.get(), task.attributes());
                if (result.hasAttribute(Task.ATT_BORDER_TAINT)) {
                    var cui = task.session().cui();
                    cui.sendMessage("terraform.warn.border_exceeded");
                }

                var postApplyFunc = task.postApplyFunc();
                if (postApplyFunc != null) {
                    postApplyFunc.exec(result);
                }
            } catch (Throwable t) {
                logger.error("{}: apply failed", task, t);
                task.setState(Task.State.FAILED);
            }
        };
    }

}
