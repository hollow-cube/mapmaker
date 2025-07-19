package net.hollowcube.terraform.task;

import com.google.gson.JsonObject;
import net.hollowcube.terraform.buffer.BlockBuffer;
import net.hollowcube.terraform.session.LocalSession;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.time.Instant;

/**
 * A task is a single action being executed by Terraform. It consists of two phases: compute and apply.
 *
 * <p>The compute phase is optional, and used to asynchronously create a {@link BlockBuffer} from
 * a snapshot of the world.</p>
 *
 * <p>The apply phase is required, and responsible for writing the {@link BlockBuffer} changes to
 * the world. During application, the changes are written to the history of the player applying.</p>
 */
public interface Task {

    @NotNull
    String ATT_BORDER_TAINT = "terraform:border_taint";

    enum State {
        INIT,
        QUEUED,
        COMPUTE,
        APPLY,
        COMPLETE,
        FAILED,
        CANCELLED;

        public boolean isTerminal() {
            return this == COMPLETE || this == FAILED || this == CANCELLED;
        }
    }

    /**
     * Returns the local session associated with this task
     */
    @NotNull LocalSession session();

    /**
     * Returns the unique (random) ID of the task
     */
    @NotNull String id();

    /**
     * Returns the tag of the task (eg set, replace, undo, redo)
     */
    @NotNull String tag();

    @NotNull Instant created();

    boolean isDryRun();

    @NotNull State state();

    /**
     * Source contains some metadata about the source of the task.
     *
     * <p>For example, Axiom tasks will have the axiom tool info, set will have pattern details, etc.</p>
     */
    @NotNull JsonObject source();

    /**
     * Returns the {@link BlockBuffer} associated with the task, or null if the task has not
     * finished the compute phase. A null check is equivalent tochecking whether {@link #state()} >= APPLY.
     */
    @UnknownNullability BlockBuffer buffer();

    void addAttribute(@NotNull String attribute);

    class Builder {
        private final LocalSession session;
        private final String tag;

        private boolean dry = false;
        private boolean ephemeral = false;

        private ComputeFunc computeFunc = null;
        private BlockBuffer buffer = null;
        private PostApplyFunc postApplyFunc = null;

        @ApiStatus.Internal
        public Builder(@NotNull LocalSession session, @NotNull String tag) {
            this.session = session;
            this.tag = tag;
        }

        public @NotNull Builder metadata() {
            //todo
            return this;
        }

        public @NotNull Builder compute(@NotNull ComputeFunc computeFunc) {
            this.computeFunc = computeFunc;
            return this;
        }

        public @NotNull Builder buffer(@NotNull BlockBuffer buffer) {
            this.buffer = buffer;
            return this;
        }

        public @NotNull Builder post(@NotNull PostApplyFunc postApplyFunc) {
            this.postApplyFunc = postApplyFunc;
            return this;
        }

        public @NotNull Builder ephemeral() {
            this.ephemeral = true;
            return this;
        }

        public @Nullable Task dryRunIfCapacity() {
            this.dry = true;
            return submitIfCapacity();
        }

        public @Nullable Task submitIfCapacity() {
            Check.argCondition(computeFunc == null && buffer == null, "Must provide either a compute function or a buffer");
            Check.argCondition(computeFunc != null && buffer != null, "Cannot provide both a compute function and a buffer");

            try {
                var task = new TaskImpl(session, tag, dry, ephemeral, computeFunc, buffer, postApplyFunc);
                session.submitTask(task);
                return task;
            } catch (TooManyTasksException ignored) {
                return null;
            }
        }

        public @Nullable Task submitForce() {
            Check.argCondition(computeFunc == null && buffer == null,
                               "Must provide either a compute function or a buffer");
            Check.argCondition(computeFunc != null && buffer != null,
                               "Cannot provide both a compute function and a buffer");

            var task = new TaskImpl(session, tag, dry, ephemeral, computeFunc, buffer, postApplyFunc);
            session.submitTaskForce(task);
            return task;
        }
    }

}
