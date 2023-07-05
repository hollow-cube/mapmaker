package net.hollowcube.terraform.session;

import net.hollowcube.terraform.cui.ClientInterface;
import net.hollowcube.terraform.cui.ClientRenderer;
import net.hollowcube.terraform.selection.Selection;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static net.minestom.server.network.NetworkBuffer.SHORT;
import static net.minestom.server.network.NetworkBuffer.VAR_INT;

/**
 * Represents a player's Terraform session, responsible for holding all "global" state.
 * <br/>
 * The meaning of "global" and "local" are somewhat dependent on the context. For example,
 * in MapMaker, the global state corresponds to a user, independent of the map they are
 * currently in. Local state is specific to the map they are in.
 * <br/>
 * todo are clipboards really going to be global?
 */
@SuppressWarnings({"UnstableApiUsage"})
public class PlayerSession {
    public static final int STATE_VERSION = 1;

    public static final Tag<PlayerSession> TAG = Tag.Transient("terraform:player_session");

    public static @NotNull PlayerSession forPlayer(@NotNull Player player) {
        var session = player.getTag(TAG);
        if (session == null) {
            session = new PlayerSession(player);
            player.setTag(TAG, session);
        }
        return session;
    }

    public static @NotNull PlayerSession load(@NotNull Player player, byte[] data) {
        //todo do we want to overwrite the session if there is one present?
        var session = new PlayerSession(player, data);
        player.setTag(TAG, session);
        return session;
    }

    public static byte @NotNull [] save(@NotNull Player player) {
        return forPlayer(player).write();
    }

    private final Player player;

    private final Map<String, Clipboard> clipboards = new HashMap<>();

    PlayerSession(@NotNull Player player) {
        this(player, null);
    }

    PlayerSession(@NotNull Player player, byte @Nullable [] data) {
        this.player = player;

        if (data != null && data.length > 0) read(data);
    }

    public @NotNull Player player() {
        return player;
    }

    public @NotNull ClientInterface cui() {
        return new ClientInterface() {
            @Override
            public void sendMessage(@NotNull String key, @NotNull Object... args) {
                var componentArgs = new Component[args.length];
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if (arg instanceof Component c)
                        componentArgs[i] = c;
                    else componentArgs[i] = Component.text(args[i].toString());
                }

                player.sendMessage(Component.translatable(key, componentArgs));
            }

            @Override
            public @NotNull ClientRenderer renderer() {
                return ClientRenderer.noop();
            }
        };
    }

    // Clipboard

    /**
     * Checks if the player has a clipboard with the given name.
     *
     * @see #clipboard(String) #clipboard(String) for details about clipboard lifecycle
     *
     * @param name The case-insensitive name of the clipboard to check
     * @return True if there is an active clipboard with the given name, false otherwise
     */
    public boolean hasClipboard(@NotNull String name) {
        clipboards.values().removeIf(Clipboard::isEmpty);
        return clipboards.containsKey(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns the clipboard with the given name, creating an empty one if it does not exist.
     * <br/>
     * A clipboard is considered "active" if it has blocks stored in it, this can be checked with
     * {@link Clipboard#isEmpty()}. Empty clipboards will be removed automatically, and will never
     * show up in related get commands.
     *
     * @implNote There is a race condition here. If this function is called, but the result
     * clipboard is not filled immediately, it could be removed if empty clipboards are pruned.
     * This can be fixed by hiding but not removing inactive clipboards (until save).
     *
     * @param name The clipboard name to fetch
     * @return The clipboard with the given name, or a new empty one
     */
    public @NotNull Clipboard clipboard(@NotNull String name) {
        name = name.toLowerCase(Locale.ROOT);
        Check.argCondition(!name.matches(Clipboard.NAME_REGEX), "Invalid clipboard name: {0}", name);
        return clipboards.computeIfAbsent(name, Clipboard::new);
    }

    /**
     * Returns an immutable set of _active_ clipboard names in this session.
     */
    public @NotNull Set<String> clipboardNames() {
        clipboards.values().removeIf(Clipboard::isEmpty);
        return Set.copyOf(clipboards.keySet());
    }


    // Serialization
    //todo document this format somewhere

    private byte @NotNull [] write() {
        //todo compress
        return NetworkBuffer.makeArray(buffer -> {
            buffer.write(SHORT, (short) STATE_VERSION);

            buffer.writeCollection(clipboards.values(), (b, c) -> c.write(b));
        });
    }

    private void read(byte @NotNull [] data) {
        var buffer = new NetworkBuffer(ByteBuffer.wrap(data));

        var version = buffer.read(SHORT);
        Check.argCondition(version > STATE_VERSION, "Cannot deserialize future session state format");

        var clipboards = buffer.readCollection(Clipboard::new);
        clipboards.forEach(c -> this.clipboards.put(c.name().toLowerCase(Locale.ROOT), c));

    }

}
