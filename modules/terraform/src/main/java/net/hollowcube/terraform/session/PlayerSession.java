package net.hollowcube.terraform.session;

import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.terraform.Terraform;
import net.hollowcube.terraform.cui.ClientInterface;
import net.hollowcube.terraform.cui.ClientRenderer;
import net.hollowcube.terraform.cui.vanilla.DefaultClientRenderer;
import net.hollowcube.terraform.selection.Selection;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.hollowcube.terraform.util.ProtocolUtil.assertMarker;
import static net.hollowcube.terraform.util.ProtocolUtil.insertMarker;
import static net.minestom.server.network.NetworkBuffer.SHORT;

/**
 * Represents a player's Terraform session, responsible for holding all "global" state.
 * <br/>
 * The meaning of "global" and "local" are somewhat dependent on the context. For example,
 * in MapMaker, the global state corresponds to a user, independent of the map they are
 * currently in. Local state is specific to the map they are in.
 * <br/>
 * todo are clipboards really going to be global?
 */
public class PlayerSession {
    @ApiStatus.Internal
    public static final Tag<PlayerSession> TAG = Tag.Transient("terraform:player_session");
    private static final int ABSOLUTE_MAX_CLIPBOARDS = 1024;
    private static final int STATE_VERSION = 1;
    private final Terraform terraform;
    private final String id;
    private final Player player;
    private final ClientInterface cui;
    private final Map<String, Clipboard> clipboards = new HashMap<>();
    private PlayerCapabilities capabilities;
    private @NotNull ClientRenderer renderer = ClientRenderer.noop();

    @ApiStatus.Internal
    public PlayerSession(@NotNull Terraform terraform, @NotNull String id, @NotNull Player player, byte @Nullable [] data) {
        this.terraform = terraform;
        this.id = id;
        this.player = player;
        this.cui = new DefaultClientInterface();
        if (data != null && data.length > 0) read(data);
    }

    /**
     * Returns the {@link PlayerSession} for the given player.
     *
     * @throws NullPointerException if the player does not have a session.
     */
    public static @NotNull PlayerSession forPlayer(@NotNull Player player) {
        return Objects.requireNonNull(player.getTag(TAG), "Player session not initialized");
    }

    public @NotNull Terraform terraform() {
        return terraform;
    }

    public @NotNull String id() {
        return id;
    }

    public @NotNull Player player() {
        return player;
    }

    public @NotNull ClientInterface cui() {
        return cui;
    }

    // Clipboard

    /**
     * Checks if the player has a clipboard with the given name.
     *
     * @param name The case-insensitive name of the clipboard to check
     * @return True if there is an active clipboard with the given name, false otherwise
     * @see #clipboard(String) #clipboard(String) for details about clipboard lifecycle
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
     * @param name The clipboard name to fetch
     * @return The clipboard with the given name, or a new empty one
     * @implNote There is a race condition here. If this function is called, but the result
     * clipboard is not filled immediately, it could be removed if empty clipboards are pruned.
     * This can be fixed by hiding but not removing inactive clipboards (until save).
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
    // Note: No data is compressed. A storage implementation MAY choose to compress the data before writing it.

    @ApiStatus.Internal
    public byte @NotNull [] write() {
        return NetworkBuffer.makeArray(buffer -> {
            buffer.write(SHORT, (short) STATE_VERSION);

            buffer.write(Clipboard.NETWORK_TYPE.list(), List.copyOf(clipboards.values()));
            insertMarker(buffer);
        });
    }

    @ApiStatus.Internal
    public void read(byte @NotNull [] data) {
        var buffer = NetworkBuffer.wrap(data, 0, data.length);

        var version = buffer.read(SHORT);
        Check.argCondition(version > STATE_VERSION, "Cannot deserialize future session state format");

        var clipboards = buffer.read(Clipboard.NETWORK_TYPE.list(ABSOLUTE_MAX_CLIPBOARDS));
        clipboards.forEach(c -> this.clipboards.put(c.name().toLowerCase(Locale.ROOT), c));
        assertMarker(buffer, "clipboards");

        assert buffer.readableBytes() == 0 : "Buffer not fully read";
    }

    public void updateRenderer() {
        var isDefaultRenderer = this.renderer instanceof DefaultClientRenderer;
        if (PlayerData.fromPlayer(player).getSetting(PlayerSettings.ENABLE_WE_CUI)) {
            if (!isDefaultRenderer) {
                this.renderer = new DefaultClientRenderer(player);

                var selection = LocalSession.forPlayer(player).selection(Selection.DEFAULT).region();
                if (selection != null) {
                    this.renderer.switchTo(ClientRenderer.RenderContext.NORMAL, false);
                    this.renderer.begin("cuboid");
                    this.renderer.cuboid(selection.min(), selection.max(), ClientRenderer.RenderType.PRIMARY);
                }
            }
        } else if (isDefaultRenderer) {
            this.renderer.clearAll();
            this.renderer = ClientRenderer.noop();
        }
    }

    private class DefaultClientInterface implements ClientInterface {
        @Override
        public void sendMessage(@NotNull String key, @NotNull Object... args) {
            var componentArgs = new Component[args.length];
            for (var i = 0; i < args.length; i++) {
                var arg = args[i];
                if (arg instanceof Component c)
                    componentArgs[i] = c;
                else componentArgs[i] = Component.text(args[i].toString());
            }

            player.sendMessage(Component.translatable(key, componentArgs));
        }

        @Override
        public @NotNull ClientRenderer renderer() {
            updateRenderer();
            return renderer;
        }
    }

}
