package net.hollowcube.terraform.session;

import net.hollowcube.terraform.Terraform;
import net.hollowcube.terraform.cui.ClientInterface;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.history.Change;
import net.hollowcube.terraform.task.Task;
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

import java.nio.ByteBuffer;
import java.util.*;

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
    private static final int STATE_VERSION = 1;

    // todo: turn these into player capabilities
    private static final int MAX_HISTORY_SIZE = 10;

    public static @NotNull LocalSession forPlayer(@NotNull Player player) {
        var instance = player.getInstance();
        var tag = Tag.<LocalSession>Transient(String.format("terraform:session/%s", player.getUuid()));
        return Objects.requireNonNull(instance.getTag(tag), "Local session not initialized");
    }

    private final PlayerSession playerSession;
    private final String id;
    private final Instance instance;

    private final Map<String, Selection> selections = new HashMap<>();

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


    // Action

    public @NotNull Task.Builder buildTask(@NotNull String tag) {
        return new Task.Builder(this, tag);
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
            buffer.writeCollection(selections.values(), (b, s) -> s.write(b));
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
        var buffer = new NetworkBuffer(ByteBuffer.wrap(data));

        var version = buffer.read(SHORT);
        Check.argCondition(version > STATE_VERSION, "Cannot deserialize future session state format");

        // Selections
        var selections = buffer.readCollection(b -> new Selection(this, b));
        selections.forEach(s -> this.selections.put(s.name(), s));
        assertMarker(buffer, "selections");

        // History
//        this.historyPointer = buffer.read(VAR_INT);
        //todo

        assert buffer.readableBytes() == 0 : "Buffer not fully read";
    }
}
