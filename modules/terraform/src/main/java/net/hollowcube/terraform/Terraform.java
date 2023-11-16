package net.hollowcube.terraform;

import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.session.PlayerSession;
import net.hollowcube.terraform.storage.TerraformStorage;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * TerraformV2 is the root of a Terraform instance.
 * <p>
 * It should be created once for an entire server
 */
public sealed interface Terraform permits TerraformImpl {
    @NotNull TerraformModule BASE_MODULE = new BaseModule();

    static @NotNull Builder builder() {
        return new Builder();
    }


    @NotNull TerraformRegistry registry();

    @NotNull TerraformStorage storage();


    // Sessions
    // todo: By default terraform should auto-init a player session when you join the game (pre login event)
    // todo: By default terraform should auto-init a local session when you enter an instance

    /**
     * Creates a {@link PlayerSession} for the given {@link Player},
     * loading data from storage if available.
     *
     * <p>This call may block the current thread, it is the callers responsibility to call it in a safe point.</p>
     */
    @Blocking
    void initPlayerSession(@NotNull Player player, @NotNull String playerId);

    @Blocking
    void savePlayerSession(@NotNull Player player, boolean drop);

    /**
     * Creates a {@link LocalSession} for the given {@link Player}, loading data from storage if available.
     *
     * <p>The player must already have a {@link PlayerSession} and be inside an {@link Instance}</p>
     *
     * <p>This call may block the current thread, it is the callers responsibility to call it in a safe point.</p>
     */
    @Blocking
    void initLocalSession(@NotNull Player player, @NotNull String sessionId);

    @Blocking
    void saveLocalSession(@NotNull Player player, boolean drop);


    class Builder {
        private final List<TerraformModule> modules = new ArrayList<>();

        public @NotNull Builder module(@NotNull TerraformModule module) {
            modules.add(module);
            return this;
        }

        public @NotNull Terraform build() {
            return new TerraformImpl(List.copyOf(modules));
        }
    }
}
