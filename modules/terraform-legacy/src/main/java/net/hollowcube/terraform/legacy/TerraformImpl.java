package net.hollowcube.terraform.legacy;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandManager;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.schem.util.GameDataProvider;
import net.hollowcube.terraform.apply.ApplyEngine;
import net.hollowcube.terraform.apply.PartialApplyException;
import net.hollowcube.terraform.apply.UnloadedChunkPolicy;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.legacy.buffer.EditBlockBuffer;
import net.hollowcube.terraform.legacy.schem.SchemUpgrader;
import net.hollowcube.terraform.legacy.session.LocalSession;
import net.hollowcube.terraform.legacy.session.PlayerSession;
import net.hollowcube.terraform.legacy.session.history.Change;
import net.hollowcube.terraform.legacy.storage.TerraformStorage;
import net.hollowcube.terraform.legacy.task.Task;
import net.hollowcube.terraform.legacy.task.TaskImpl;
import net.hollowcube.terraform.legacy.task.TaskResult;
import net.hollowcube.terraform.legacy.task.edit.WorldView;
import net.hollowcube.terraform.legacy.tool.ToolHandler;
import net.hollowcube.terraform.legacy.util.Format;
import net.hollowcube.terraform.legacy.util.ThreadUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static net.hollowcube.mapmaker.player.LocalPlayer.localPlayer;

@SuppressWarnings("UnstableApiUsage")
@ApiStatus.Internal
public final class TerraformImpl implements Terraform {
    private static final Logger logger = LoggerFactory.getLogger(Terraform.class);

    final EventNode<InstanceEvent> eventNode;
    private final TerraformRegistryImpl registry;
    private final TerraformStorage storage;

    private final ToolHandler toolHandler;

    private volatile boolean queueLock = false;

    // Tasks
    private final ExecutorService threadPoolCompute;
    private final ExecutorService threadPoolApply;
    private final ApplyEngine applyEngine;

    TerraformImpl(
            @NotNull Collection<Supplier<TerraformModule>> modules,
            @NotNull String storage, @Nullable EventNode<InstanceEvent> eventNode,
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

        // Construct the registry immediately
        this.registry = new TerraformRegistryImpl(modules, this.eventNode, commandManager, commandCondition);

        this.applyEngine = ApplyEngine.builder()
                .blockHandlers(blockHandlers(this.registry))
                .heightmap(this.registry.heightmaps())
                .build();

        this.storage = Objects.requireNonNull(this.registry.storage(storage),
                "No matching storage implementation: " + storage);

        this.threadPoolCompute = Executors.newFixedThreadPool(3, new ThreadUtil.NamedThreadFactory("tf-compute"));
        this.threadPoolApply = Executors.newFixedThreadPool(3, new ThreadUtil.NamedThreadFactory("tf-apply"));

        GameDataProvider.replaceGlobals(SchemUpgrader.INSTANCE);
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

    @Override
    public boolean queueLockState() {
        return queueLock;
    }

    @Override
    public void setQueueLockState(boolean state) {
        queueLock = state;
    }

    // Sessions

    @Override
    public void initPlayerSession(@NotNull Player player, @NotNull String playerId) {
        Check.stateCondition(player.hasTag(PlayerSession.TAG), "Player already has a session");

        var sessionData = storage.loadPlayerSession(playerId);
        var session = new PlayerSession(this, playerId, player, sessionData);
        player.setTag(PlayerSession.TAG, session);
        logger.debug("Created session for {} ({}) withData={}", player.getUuid(), player.getUsername(),
                sessionData != null && sessionData.length > 0);
    }

    @Override
    public void savePlayerSession(@NotNull Player player, boolean drop) {
        var session = drop
                ? player.getAndSetTag(PlayerSession.TAG, null)
                : player.getTag(PlayerSession.TAG);
        Check.notNull(session, "Player does not have a session: {0}", player.getUsername());

        var sessionData = session.write();
        storage.savePlayerSession(session.id(), sessionData);
        logger.debug("Saved session for {} ({}) drop={}, size={}", player.getUuid(), player.getUsername(),
                drop, Format.formatBytes(sessionData.length));
    }

    @Override
    public void initLocalSession(@NotNull Player player, @NotNull Instance instance, @NotNull String sessionId) {
        var existing = LocalSession.forPlayerOptional(player);
        if (existing != null && !sessionId.equals(existing.id())) {
            throw new IllegalStateException("Player already has a local session with a different id: "
                    + player.getUsername() + " (" + existing.id() + " != " + sessionId + ")");
        }

        var playerSession = PlayerSession.forPlayer(player);
        var sessionData = storage.loadLocalSession(playerSession.id(), sessionId);
        player.setTag(LocalSession.TAG, new LocalSession(playerSession, sessionId, instance, sessionData));
        logger.debug("Created local session for {} ({}) withData={}", player.getUuid(), player.getUsername(),
                sessionData != null && sessionData.length > 0);
    }

    @Override
    public void saveLocalSession(@NotNull Player player, @NotNull Instance instance, boolean drop) {
        var tag = Tag.<LocalSession>Transient(String.format("terraform:session/%s", player.getUuid()));

        var session = instance.getTag(tag);
        if (session == null) {
            logger.warn("No local session found for {} ({})", player.getUuid(), player.getUsername());
            return;
        }

        var sessionData = session.write();
        storage.saveLocalSession(session.playerId(), session.id(), sessionData);
        if (drop) instance.removeTag(tag);
        logger.debug("Saved local session for {} ({}) drop={}, size={}", player.getUuid(), player.getUsername(),
                drop, Format.formatBytes(sessionData.length));
    }


    @Override
    public @NotNull CompletableFuture<Void> closeInstance(@NotNull Instance instance) {
        return applyEngine.close(instance);
    }

    // Internal Task API

    /**
     * Queues a task to have its compute phase executed, then its apply phase.
     */
    public void submitTask(@NotNull TaskImpl task) {
        if (task.state() != Task.State.INIT) return;

        if (queueLock) {
            logger.debug("{}: queue locked", task);
            task.setState(Task.State.CANCELLED, null);
            task.session().cui().sendMessage("terraform.error.queue_locked");
            return;
        }

        logger.debug("{}: submitted", task);
        task.setState(Task.State.QUEUED, task.computeFunc() != null
                ? threadPoolCompute.submit(computeTask(task))
                : threadPoolApply.submit(applyTask(task)));
    }

    private @NotNull Runnable computeTask(@NotNull TaskImpl task) {
        return () -> {
            try {
                logger.debug("{}: compute started", task);
                task.setState(Task.State.COMPUTE, null);
                long start = System.nanoTime();

                var world = WorldView.instance(task, task.session().instance());
                var buffer = Objects.requireNonNull(task.computeFunc()).exec(task, world);
                task.setBuffer(buffer);
                ThreadUtil.testInterrupt(); // Stop before submitting the apply task

                task.setComputeNanos(System.nanoTime() - start);
                logger.debug("{}: compute complete in {}ms ({})", task, task.computeNanos() / 1_000_000d, Format.formatBytes(buffer.sizeBytes()));
                task.setState(Task.State.QUEUED, threadPoolApply.submit(applyTask(task)));
            } catch (InterruptedException interrupt) {
                task.setState(Task.State.CANCELLED, null);
                logger.debug("{}: compute cancelled", task);
                Thread.currentThread().interrupt();
            } catch (Throwable t) {
                logger.error("{}: compute failed", task, t);
                task.setState(Task.State.FAILED, null);
            }
        };
    }

    private @NotNull Runnable applyTask(@NotNull TaskImpl task) {
        Objects.requireNonNull(task.buffer(), "Task buffer must not be null when applying");
        return () -> {
            try {
                logger.debug("{}: apply started", task);
                task.setState(Task.State.APPLY, null);
                var timings = new ApplyTimings(System.nanoTime());

                var instance = task.session().instance();
                var edit = clipToBorder(instance, EditBlockBuffer.toEditBuffer(task.buffer()));
                timings.converted = System.nanoTime();

                if (task.isDryRun()) {
                    var read = EditBuffer.builder();
                    long changed = readTouched(instance, edit, read);
                    timings.captured = timings.applied = timings.relit = System.nanoTime();
                    complete(task, new EditBlockBuffer(read.build()), new EditBlockBuffer(edit), changed, timings);
                    return;
                }

                EditBuffer before = null;
                if (task.capturesAll()) {
                    var read = EditBuffer.builder();
                    readTouched(instance, edit, read);
                    before = read.build();
                }
                timings.captured = System.nanoTime();

                var capturedBefore = before;
                applyEngine.submit(instance, edit, UnloadedChunkPolicy.LOAD, !task.isEphemeral())
                        .whenComplete((result, error) -> onApplied(task, instance, edit, capturedBefore, result, error, timings));
            } catch (Throwable t) {
                logger.error("{}: apply failed", task, t);
                task.setState(Task.State.FAILED, null);
            }
        };
    }

    /**
     * Runs on the instance tick (or the submitting thread if the apply failed before queueing). History is
     * recorded here, on the same thread as {@code //undo}. Relighting and the task's callbacks go back to the
     * apply pool, keeping light off the tick.
     */
    private void onApplied(
            @NotNull TaskImpl task, @NotNull Instance instance, @NotNull EditBuffer edit, @Nullable EditBuffer before,
            @Nullable ApplyEngine.ApplyResult result, @Nullable Throwable error, @NotNull ApplyTimings timings
    ) {
        try {
            timings.applied = System.nanoTime();
            if (error != null) {
                // Keep what was written undoable, then fail the task.
                var partial = error instanceof PartialApplyException e ? e.partial() : null;
                if (partial != null && !task.isEphemeral()) {
                    var partialUndo = new EditBlockBuffer(before != null ? before : partial.undo());
                    task.session().remember(Change.of(partialUndo, new EditBlockBuffer(edit)));
                }
                threadPoolApply.execute(() -> {
                    if (partial != null) relight(instance, partial.chunks());
                    logger.error("{}: apply failed", task, error);
                    task.setState(Task.State.FAILED, null);
                });
                return;
            }

            var undoBuffer = new EditBlockBuffer(before != null ? before : Objects.requireNonNull(result).undo());
            var redoBuffer = new EditBlockBuffer(edit);
            if (!task.isEphemeral()) {
                task.session().remember(Change.of(undoBuffer, redoBuffer));
            }
            threadPoolApply.execute(() -> {
                try {
                    relight(instance, result.chunks());
                    timings.relit = System.nanoTime();
                    complete(task, undoBuffer, redoBuffer, result.changed(), timings);
                } catch (Throwable t) {
                    logger.error("{}: apply failed", task, t);
                    task.setState(Task.State.FAILED, null);
                }
            });
        } catch (Throwable t) {
            logger.error("{}: apply failed", task, t);
            task.setState(Task.State.FAILED, null);
        }
    }

    private void complete(
            @NotNull TaskImpl task, @NotNull EditBlockBuffer undoBuffer, @NotNull EditBlockBuffer redoBuffer,
            long changed, @NotNull ApplyTimings timings
    ) {
        task.setState(Task.State.COMPLETE, null);
        logger.debug("{}: apply complete in {}ms", task, (System.nanoTime() - timings.start) / 1_000_000d);
        reportStats(task, redoBuffer.edit(), undoBuffer.edit(), changed, timings);

        var result = new TaskResult(undoBuffer, redoBuffer, changed, task.attributes());
        if (result.hasAttribute(Task.ATT_BORDER_TAINT)) {
            var cui = task.session().cui();
            cui.sendMessage("terraform.warn.border_exceeded");
        }

        var postApplyFunc = task.postApplyFunc();
        if (postApplyFunc != null) {
            postApplyFunc.exec(result);
        }
    }

    /**
     * When each stage of an apply ended, in {@link System#nanoTime()}. Written by one thread at a time, handed on
     * through the executors.
     */
    private static final class ApplyTimings {
        final long start;
        long converted, captured, applied, relit;

        ApplyTimings(long start) {
            this.start = start;
        }
    }

    /**
     * Tells a player who turned on {@link PlayerSettings#TERRAFORM_STATS} how long each stage of the task took
     * and how much it changed.
     */
    private static void reportStats(
            @NotNull TaskImpl task, @NotNull EditBuffer edit, @NotNull EditBuffer undo, long changed,
            @NotNull ApplyTimings timings
    ) {
        long applyStart = timings.start, converted = timings.converted, captured = timings.captured;
        long applied = timings.applied, relit = timings.relit;
        var player = task.session().parent().player();
        if (!localPlayer(player).getSetting(PlayerSettings.TERRAFORM_STATS)) return;

        long wall = relit - task.createdNanos();
        long compute = task.computeNanos();
        long queued = wall - compute - (relit - applyStart);
        double seconds = Math.max(wall, 1) / 1e9;
        var message = String.format(
                "[tf] %s: %dms wall (queued %d, compute %d, convert %d, capture %d, apply %d, relight %d) | %,d changed, %,.0f/s | edit %s, undo %s",
                task.tag(), wall / 1_000_000, queued / 1_000_000, compute / 1_000_000,
                (converted - applyStart) / 1_000_000, (captured - converted) / 1_000_000,
                (applied - captured) / 1_000_000, (relit - applied) / 1_000_000,
                changed, changed / seconds, Format.formatBytes(edit.sizeBytes()), Format.formatBytes(undo.sizeBytes())
        );
        player.sendMessage(Component.text(message, NamedTextColor.GRAY));
    }

    /**
     * Drops every cell outside the world border and height, the box {@code WorldViewInstance#fastBorderTest}
     * accepts ({@code <} on the positive side).
     */
    private static @NotNull EditBuffer clipToBorder(@NotNull Instance instance, @NotNull EditBuffer edit) {
        var border = instance.getWorldBorder();
        var dimension = instance.getCachedDimensionType();
        double radius = border.diameter() / 2d;
        return edit.clipToBox(
                (int) Math.ceil(border.centerX() - radius),
                dimension.minY(),
                (int) Math.ceil(border.centerZ() - radius),
                (int) Math.ceil(border.centerX() + radius) - 1,
                dimension.maxY() - 1,
                (int) Math.ceil(border.centerZ() + radius) - 1
        );
    }

    /**
     * Reads the world's current block at every cell the edit sets into {@code out}, returning how many of
     * them the edit would change. A dry run's stand-in for the undo buffer.
     */
    private static long readTouched(@NotNull Instance instance, @NotNull EditBuffer edit, @NotNull EditBuffer.Builder out) {
        long[] changed = {0};
        edit.forEachSection((sectionX, sectionY, sectionZ, section) -> {
            var chunk = instance.getChunk(sectionX, sectionZ);
            if (chunk == null) chunk = instance.loadChunk(sectionX, sectionZ).join();
            if (chunk == null || sectionY < chunk.getMinSection() || sectionY >= chunk.getMaxSection()) return;

            final var chunkRef = chunk;
            int baseX = sectionX << 4, baseY = sectionY << 4, baseZ = sectionZ << 4;
            chunk.lockReadLock();
            try {
                section.forEachBlockState((x, y, z, stateId) -> {
                    var old = chunkRef.getBlock(baseX + x, baseY + y, baseZ + z);
                    out.setBlockState(baseX + x, baseY + y, baseZ + z, old);
                    if (old.stateId() != stateId) changed[0]++;
                });
            } finally {
                chunk.unlockReadLock();
            }
        });
        return changed[0];
    }

    private static @NotNull Map<Key, BlockHandler> blockHandlers(@NotNull TerraformRegistry registry) {
        var handlers = new HashMap<Key, BlockHandler>();
        for (int stateId = 0; stateId < Block.statesCount(); stateId++) {
            var block = registry.blockState(stateId);
            if (block != null && block.handler() != null) handlers.putIfAbsent(block.key(), block.handler());
        }
        return handlers;
    }

    /**
     * Relights the chunks an apply wrote, below {@code setBlock}, and resends their light. Runs on the apply
     * thread, never the tick. A failure only leaves light stale, so it is logged rather than failing the task.
     */
    private static void relight(@NotNull Instance instance, @NotNull List<Chunk> chunks) {
        try {
            for (Chunk chunk : LightingChunk.relight(instance, chunks)) {
                if (chunk instanceof LightingChunk lighting) lighting.sendLighting();
            }
        } catch (RuntimeException e) {
            logger.warn("relight failed for instance {}", instance.getUuid(), e);
        }
    }

}
