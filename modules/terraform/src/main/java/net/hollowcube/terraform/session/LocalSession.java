package net.hollowcube.terraform.session;

import net.hollowcube.terraform.Terraform;
import net.hollowcube.terraform.TerraformImpl;
import net.hollowcube.terraform.cui.ClientInterface;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.history.Change;
import net.hollowcube.terraform.task.Task;
import net.hollowcube.terraform.task.TaskImpl;
import net.hollowcube.terraform.task.TooManyTasksException;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static net.hollowcube.terraform.util.ProtocolUtil.assertMarker;
import static net.hollowcube.terraform.util.ProtocolUtil.insertMarker;
import static net.minestom.server.network.NetworkBuffer.SHORT;

/**
 * A Terraform session local to a world. Stores only information relevant to the
 * world and defers to a {@link PlayerSession} for everything else.
 * <p>
 * LocalSessions can be serialized to/from a binary blob. The format is an implementation
 * detail and should NOT be depended upon. Limits should be imposed using capabilities (todo)
 *
 * @implNote {@link LocalSession}s will be stored with editing world savestates.
 */
@SuppressWarnings("UnstableApiUsage")
public class LocalSession {
    private static final Logger logger = LoggerFactory.getLogger(LocalSession.class);
    public static final Tag<LocalSession> TAG = Tag.Transient("terraform:local_session");

    private static final int STATE_VERSION = 1;

    // todo: turn these into player capabilities
    private static final int MAX_HISTORY_SIZE = 10;
    private static final int ABSOLUTE_MAX_SELECTIONS = 1024;
    private static final int MAX_QUEUED_TASKS = 5;

    public static @NotNull LocalSession forPlayer(@NotNull Player player) {
        return Objects.requireNonNull(forPlayerOptional(player), "Local session not initialized");
    }

    public static @Nullable LocalSession forPlayerOptional(@NotNull Player player) {
        return player.getTag(TAG);
    }

    private final PlayerSession playerSession;
    private final String id;
    private final Instance instance;

    private final Map<String, Selection> selections = new HashMap<>();

    private final ReentrantLock taskLock = new ReentrantLock();
    private final List<TaskImpl> tasks = new ArrayList<>();

    private final List<Change> history = new ArrayList<>();
    private int historyPointer = 0;

    public LocalSession(@NotNull PlayerSession playerSession, @NotNull String id, @NotNull Instance instance, byte @Nullable [] data) {
        this.playerSession = playerSession;
        this.id = id;
        this.instance = instance;

        selections.put(Selection.DEFAULT, new Selection(this, Selection.DEFAULT));

        // Read the existing data if it was provided
        if (data != null && data.length != 0) deserialize(data);
    }

    public @NotNull Terraform terraform() {
        return playerSession.terraform();
    }

    public @NotNull PlayerSession parent() {
        return playerSession;
    }

    public @NotNull String id() {
        return id;
    }

    public @NotNull String playerId() {
        return playerSession.id();
    }

    public @NotNull Instance instance() {
        return instance;
    }

    public @NotNull ClientInterface cui() {
        return playerSession.cui();
    }


    // Selection

    public boolean hasSelection(@NotNull String name) {
        return selections.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public @NotNull Selection selection(@NotNull String name) {
        return selections.computeIfAbsent(name.toLowerCase(Locale.ROOT),
                n -> new Selection(this, n));
    }

    public @NotNull Collection<String> selectionNames() {
        return Set.copyOf(selections.keySet());
    }


    // Tasks

    public @NotNull List<Task> tasks() {
        return List.copyOf(tasks);
    }

    public @NotNull Task.Builder buildTask(@NotNull String tag) {
        return new Task.Builder(this, tag);
    }

    @ApiStatus.Internal
    public void submitTask(@NotNull Task task) {
        taskLock.lock();
        try {
            if (tasks.size() >= MAX_QUEUED_TASKS)
                throw new TooManyTasksException();

            var taskInternal = (TaskImpl) task;
            tasks.add(taskInternal);
            ((TerraformImpl) terraform()).submitTask(taskInternal);
        } finally {
            taskLock.unlock();
        }
    }

    @ApiStatus.Internal
    public void submitTaskForce(@NotNull Task task) {
        taskLock.lock();
        try {
            var taskInternal = (TaskImpl) task;
            tasks.add(taskInternal);
            ((TerraformImpl) terraform()).submitTask(taskInternal);
        } finally {
            taskLock.unlock();
        }
    }

    /**
     * <p>Attempts to cancel the given task managed by this {@link LocalSession}.</p>
     *
     * <p>A task may not be cancelled once the apply phase has started, and {@link Task.State#APPLY} will always
     * be returned in this case. If the task is already in a terminal state (complete, failed, cancelled), that
     * state will be returned.</p>
     *
     * @param task the task to cancel
     * @return the new state of the task
     * @throws IllegalArgumentException if the task is not managed by this session
     */
    public @NotNull Task.State cancelTask(@NotNull Task task) {
        Check.argCondition(this != task.session(), "Task not managed by this session");
        if (task.state().isTerminal() || task.state() == Task.State.APPLY) return task.state();
        var future = ((TaskImpl) task).future();
        if (future != null) future.cancel(true);
        return Task.State.CANCELLED;
    }

    @ApiStatus.Internal
    public void removeTask(@NotNull TaskImpl task) {
        taskLock.lock();
        try {
            tasks.remove(task);
        } finally {
            taskLock.unlock();
        }
    }


    // History

    public void remember(@NotNull Change change) {
        history.add(historyPointer, change);
        historyPointer++;

        // Discard all entries after the history pointer, they are from an old history chain.
        if (history.size() > historyPointer) {
            history.subList(historyPointer, history.size()).clear();
        }

        // If the history size is too large, remove the oldest entry
        if (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
            historyPointer--;
            logger.debug("Trimmed history to {} entries", history.size());
        }
    }

    public int undo(int count) {
        var undone = 0;
        for (int i = 0; i < count; i++) {
            if (historyPointer == 0) break;
            historyPointer--;
            history.get(historyPointer).undo(this);
            undone++;
        }
        return undone;
    }

    public int redo(int count) {
        var redone = 0;
        for (int i = 0; i < count; i++) {
            if (historyPointer == history.size()) break;
            history.get(historyPointer).redo(this);
            historyPointer++;
            redone++;
        }
        return redone;
    }

    public int undoCount() {
        return historyPointer;
    }

    public int redoCount() {
        return history.size() - historyPointer;
    }

    public void clearHistory() {
        historyPointer = 0;
        history.clear();
    }


    // Serialization
    // Note: No data is compressed. A storage implementation MAY choose to compress the data before writing it.

    @ApiStatus.Internal
    public byte @NotNull [] write() {
        return NetworkBuffer.makeArray(buffer -> {
            buffer.write(SHORT, (short) STATE_VERSION);

            // Selections
            buffer.write(new NetworkBuffer.Type<Selection>() {
                @Override public void write(@NotNull NetworkBuffer buffer, Selection value) {
                    value.write(buffer);
                }

                @Override public Selection read(@NotNull NetworkBuffer buffer) {
                    return null;
                }
            }.list(), List.copyOf(selections.values()));
            insertMarker(buffer);

            // History
//            buffer.write(VAR_INT, historyPointer);
//            buffer.write(VAR_INT, history.size());
//            for (var change : history) {
            //todo write the changes
//            }
        });
    }

    private void deserialize(byte @NotNull [] data) {
        var buffer = NetworkBuffer.wrap(data, 0, data.length);

        var version = buffer.read(SHORT);
        Check.argCondition(version > STATE_VERSION, "Cannot deserialize future session state format");

        // Selections
        var selections = buffer.read(new NetworkBuffer.Type<Selection>() {
            @Override public void write(@NotNull NetworkBuffer buffer, Selection value) {
            }

            @Override public Selection read(@NotNull NetworkBuffer buffer) {
                return new Selection(LocalSession.this, buffer);
            }
        }.list(ABSOLUTE_MAX_SELECTIONS));
        selections.forEach(s -> this.selections.put(s.name(), s));
        assertMarker(buffer, "selections");

        // History
        if (buffer.readableBytes() > 0) {
//            this.historyPointer = buffer.read(VAR_INT);
            //todo
        }

//        assert buffer.readableBytes() == 0 : "Buffer not fully read";
    }
}
