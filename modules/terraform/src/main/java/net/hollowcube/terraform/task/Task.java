package net.hollowcube.terraform.task;

import com.google.gson.JsonObject;
import net.hollowcube.terraform.TerraformImpl;
import net.hollowcube.terraform.buffer.BlockBuffer;
import net.hollowcube.terraform.session.LocalSession;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

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

    enum State {
        INIT, QUEUED, COMPUTE, APPLY, COMPLETE, FAILED
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

    class Builder {
        private final LocalSession session;
        private final String tag;

        private ComputeFunc computeFunc = null;
        private BlockBuffer buffer = null;

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

        public @NotNull Task submit() {
            Check.argCondition(computeFunc == null && buffer == null, "Must provide either a compute function or a buffer");
            Check.argCondition(computeFunc != null && buffer != null, "Cannot provide both a compute function and a buffer");
            var terraform = (TerraformImpl) session.terraform();

            var task = new TaskImpl(session, tag, computeFunc, buffer);
            terraform.submitTask(task);
            return task;
        }
    }

}
