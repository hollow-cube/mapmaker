package net.hollowcube.terraform.session;

import net.hollowcube.terraform.TerraformV2;
import net.hollowcube.terraform.action.ActionBuilder;
import net.hollowcube.terraform.cui.ClientInterface;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.history.Change;
import net.hollowcube.terraform.task.Task;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;

import static net.minestom.server.network.NetworkBuffer.SHORT;
import static net.minestom.server.network.NetworkBuffer.VAR_INT;

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

    public static final int STATE_VERSION = 1;

    public static @NotNull LocalSession forPlayer(@NotNull Player player) {
        var instance = player.getInstance();
        var tag = Tag.<LocalSession>Transient(String.format("terraform:session/%s", player.getUuid()));
        var session = instance.getTag(tag);
        if (session == null) {
            session = new LocalSession(PlayerSession.forPlayer(player), instance);
            instance.setTag(tag, session);
        }
        return session;
    }

    public static @NotNull LocalSession load(@NotNull Player player, byte[] data) {
        var instance = player.getInstance();
        var tag = Tag.<LocalSession>Transient(String.format("terraform:session/%s", player.getUuid()));
        var session = instance.getTag(tag);
        if (session != null) {
            logger.warn("Attempted to load a session for a player that already has one");
        }

        session = new LocalSession(PlayerSession.forPlayer(player), instance, data);
        instance.setTag(tag, session);
        return session;
    }

    public static byte @NotNull [] save(@NotNull Player player) {
        return forPlayer(player).serialize();
    }

    private final PlayerSession playerSession;
    private final Instance instance;

    private final Map<String, Selection> selections = new HashMap<>();

    private final List<Change> history = new ArrayList<>();
    private int historyPointer = 0;

    public LocalSession(@NotNull PlayerSession playerSession, @NotNull Instance instance) {
        this(playerSession, instance, null);
    }

    public LocalSession(@NotNull PlayerSession playerSession, @NotNull Instance instance, byte @Nullable [] data) {
        this.playerSession = playerSession;
        this.instance = instance;

        selections.put(Selection.DEFAULT, new Selection(this, Selection.DEFAULT));

        // Read the existing data if it was provided
        if (data != null && data.length != 0) deserialize(data);
    }

    public @NotNull TerraformV2 terraform() {
        return playerSession.terraform();
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

    public @NotNull ActionBuilder action() {
        return new ActionBuilder(this);
    }

    public @NotNull Task.Builder buildTask(@NotNull String tag) {
        return new Task.Builder(this, tag);
    }


    // History

    public void remember(@NotNull Change change) {
        history.add(historyPointer, change);
        historyPointer++;

        //todo discard all after the history pointer
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
        history.clear();
        historyPointer = 0;
    }


    // Serialization
    //todo document this format somewhere

    private byte @NotNull [] serialize() {
        //todo compress
        return NetworkBuffer.makeArray(buffer -> {
            buffer.write(SHORT, (short) STATE_VERSION);

            // Selections
            buffer.writeCollection(selections.values(), (b, s) -> s.write(b));

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
        int selectionCount = buffer.read(VAR_INT);
        for (int i = 0; i < selectionCount; i++) {
            var selection = new Selection(this, buffer);
            this.selections.put(selection.name(), selection);
        }

        // History
//        this.historyPointer = buffer.read(VAR_INT);
        //todo
    }

}
