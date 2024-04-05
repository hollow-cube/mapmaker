package net.hollowcube.terraform;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandManager;
import net.hollowcube.command.CommandManagerImpl;
import net.hollowcube.terraform.compat.axiom.AxiomModule;
import net.hollowcube.terraform.compat.worldedit.WorldEditModule;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.session.PlayerSession;
import net.hollowcube.terraform.storage.TerraformStorage;
import net.hollowcube.terraform.tool.ToolHandler;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * TerraformV2 is the root of a Terraform instance.
 * <p>
 * It should be created once for an entire server
 */
public sealed interface Terraform permits TerraformImpl {
    @NotNull
    Supplier<TerraformModule> BASE_MODULE = BaseModule::new;
    @NotNull
    Supplier<TerraformModule> WORLDEDIT_MODULE = WorldEditModule::new;
    @NotNull
    Supplier<TerraformModule> AXIOM_MODULE = AxiomModule::new;

    static @NotNull Builder builder() {
        return new Builder();
    }


    @NotNull
    TerraformRegistry registry();

    @NotNull
    TerraformStorage storage();


    // Debug state (should refactor to be a bit fancier)

    // If true, terraform will not allow tasks to be queued.
    boolean queueLockState();
    void setQueueLockState(boolean state);


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

    //todo rework how tools are handled completely
    @NotNull ToolHandler toolHandler();


    class Builder {
        private final Map<Class<?>, Object> context = new HashMap<>();

        private final List<Supplier<TerraformModule>> modules = new ArrayList<>();
        private String storage = "net.hollowcube.terraform.storage.TerraformStorageMemory";

        private EventNode<InstanceEvent> eventNode;
        private CommandManager commandManager = new CommandManagerImpl();
        private CommandCondition commandCondition = null;

        /**
         * Adds a context object to the Terraform instance. This object will be available in the Guice Injector,
         * so may be used by any component.
         *
         * @param type     The type of the object
         * @param instance The instance to add
         * @param <T>      The type of the object
         * @return this
         */
        public <T> @NotNull Builder context(@NotNull Class<T> type, @NotNull T instance) {
            context.put(type, instance);
            return this;
        }

        public @NotNull Builder module(@NotNull TerraformModule module) {
            modules.add(() -> module);
            return this;
        }

        public @NotNull Builder module(@NotNull Supplier<TerraformModule> module) {
            modules.add(module);
            return this;
        }

        public @NotNull Builder storage(@NotNull String storage) {
            this.storage = storage;
            return this;
        }

        /**
         * Sets the root event node for Terraform, may be filtered. If unset, a new event node in
         * the {@link net.minestom.server.event.GlobalEventHandler} will be created.
         *
         * <p>If set, the event node is not registered by terraform.</p>
         *
         * @param eventNode An eventNode for terraform to register listeners.
         * @return this
         */
        public @NotNull Builder rootEventNode(@NotNull EventNode<InstanceEvent> eventNode) {
            this.eventNode = eventNode;
            return this;
        }

        public @NotNull Builder rootCommandManager(@NotNull CommandManager commandManager) {
            this.commandManager = commandManager;
            return this;
        }

        /**
         * Sets a command condition to be applied to all terraform commands.
         *
         * @param commandCondition The condition to apply
         * @return this
         */
        public @NotNull Builder globalCommandCondition(@Nullable CommandCondition commandCondition) {
            this.commandCondition = commandCondition;
            return this;
        }

        public @NotNull Terraform build() {
            return new TerraformImpl(
                    context, List.copyOf(modules),
                    storage, eventNode,
                    commandManager, commandCondition
            );
        }
    }
}
