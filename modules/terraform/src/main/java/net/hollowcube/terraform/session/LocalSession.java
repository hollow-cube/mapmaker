package net.hollowcube.terraform.session;

import net.hollowcube.terraform.action.ActionBuilder;
import net.hollowcube.terraform.selection.Selection;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A Terraform session local to a world. Stores only information relevant to the
 * world and defers to a {@link PlayerSession} for everything else.
 *
 * @implNote {@link LocalSession}s will be stored with editing world savestates.
 */
public class LocalSession {
    private static LocalSession temp = null;

    public static @NotNull LocalSession forPlayer(@NotNull Player player) {
        var instance = player.getInstance();
//        var tag = ExtraTags.<LocalSession>Transient(String.format("terraform:session/%s", player.getUuid()));
        if (temp == null) {
            temp = new LocalSession(PlayerSession.fromPlayer(player), instance);
        }
//        var session = instance.getTag(tag);
//        if (session == null) {
//            session = new LocalSession(PlayerSession.fromPlayer(player), instance);
//            instance.setTag(tag, session);
//        }
        return temp;
    }

    private final PlayerSession playerSession;
    private final Instance instance;

    private final Map<String, Selection> selection = new HashMap<>();

    public LocalSession(@NotNull PlayerSession playerSession, @NotNull Instance instance) {
        this.playerSession = playerSession;
        this.instance = instance;

        selection.put(Selection.DEFAULT, new Selection(playerSession.player(), Selection.DEFAULT));
    }

    public @NotNull Instance instance() {
        return instance;
    }

    // Selection

    public @UnknownNullability Selection selection(@NotNull String name) {
        return selection.computeIfAbsent(name.toLowerCase(Locale.ROOT), n -> new Selection(playerSession.player(), n));
    }

    // Action

    public @NotNull ActionBuilder action() {
        return new ActionBuilder(this);
    }

}
