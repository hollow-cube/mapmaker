package net.hollowcube.terraform.session;

import net.hollowcube.terraform.action.ActionBuilder;
import net.hollowcube.terraform.history.Change;
import net.hollowcube.terraform.selection.Selection;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jglrxavpok.hephaistos.nbt.*;
import org.jglrxavpok.hephaistos.nbt.mutable.MutableNBTCompound;

import java.util.*;

/**
 * A Terraform session local to a world. Stores only information relevant to the
 * world and defers to a {@link PlayerSession} for everything else.
 *
 * @implNote {@link LocalSession}s will be stored with editing world savestates.
 */
public class LocalSession {

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

    private final PlayerSession playerSession;
    private final Instance instance;

    private final Map<String, Selection> selection = new HashMap<>();

    private final List<Change> history = new ArrayList<>();
    private int historyPointer = 0;

    public LocalSession(@NotNull PlayerSession playerSession, @NotNull Instance instance) {
        this.playerSession = playerSession;
        this.instance = instance;

        selection.put(Selection.DEFAULT, new Selection(playerSession.player(), Selection.DEFAULT));
    }

    public @NotNull Instance instance() {
        return instance;
    }

    // Selection

    public boolean hasSelection(@NotNull String name) {
        return selection.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public @NotNull Selection selection(@NotNull String name) {
        return selection.computeIfAbsent(name.toLowerCase(Locale.ROOT), n -> new Selection(playerSession.player(), n));
    }

    public @NotNull Collection<String> selectionNames() {
        return Set.copyOf(selection.keySet());
    }

    // Action

    public @NotNull ActionBuilder action() {
        return new ActionBuilder(this);
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

    private static class Serializer implements TagSerializer<LocalSession> {
        private static final Tag<NBT> TAG = Tag.NBT("terraform:local_session");

        @Override
        public @Nullable LocalSession read(@NotNull TagReadable reader) {
            var root = (NBTCompound) reader.getTag(TAG);

            //todo storing a reference to the player like this and assuming they are present is extremely yikes.
            // will instead store this data in some other format (dunno what) and then require that it is manually set
            // eg when loading the player
            var player = MinecraftServer.getConnectionManager().getPlayer(UUID.fromString(root.getString("owner")));
            var instance = MinecraftServer.getInstanceManager().getInstance(UUID.fromString(root.getString("instance")));
            var session = new LocalSession(PlayerSession.forPlayer(player), instance);

            // Selection
            var selections = root.getList("selections");
            for (var entry : selections) {
                var selection = Selection.fromNBT(player, (NBTCompound) entry);
                session.selection.put(selection.name(), selection);
            }

            // History
            session.historyPointer = root.getInt("history_pointer");
            var history = root.getList("history");
            for (var entry : history) {
                var change = Change.fromNBT((NBTCompound) entry);
                session.history.add(change);
            }

            return session;
        }

        @Override
        public void write(@NotNull TagWritable writer, @NotNull LocalSession value) {
            var root = new MutableNBTCompound();
            //todo see comment in `read`
            root.set("owner", new NBTString(value.playerSession.player().getUuid().toString()));
            root.set("instance", new NBTString(value.instance.getUniqueId().toString()));

            // Selection
            var selections = new ArrayList<NBTCompound>();
            for (var selection : value.selection.values()) {
                selections.add(selection.toNBT());
            }
            root.set("selections", new NBTList<>(NBTType.TAG_Compound, selections));

            // History
            root.set("history_pointer", new NBTInt(value.historyPointer));
            var history = new ArrayList<NBTCompound>();
            for (var change : value.history) {
                history.add(change.toNBT());
            }
            root.set("history", new NBTList<>(NBTType.TAG_Compound, history));

            writer.setTag(TAG, root.toCompound());
        }
    }

}
