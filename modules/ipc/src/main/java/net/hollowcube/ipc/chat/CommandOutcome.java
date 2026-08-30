package net.hollowcube.ipc.chat;

import net.hollowcube.common.util.RuntimeGson;

/// How a command ended, as the sending server's dispatch saw it.
///
/// A record around the status rather than the bare enum, so that what an outcome carries can grow:
/// a new nullable field here is a wire-compatible change, while replacing an enum on the wire with
/// anything else is not.
@RuntimeGson
public record CommandOutcome(Status status) {

    public static final CommandOutcome SUCCESS = new CommandOutcome(Status.SUCCESS);
    public static final CommandOutcome DENIED = new CommandOutcome(Status.DENIED);
    public static final CommandOutcome NOT_FOUND = new CommandOutcome(Status.NOT_FOUND);
    public static final CommandOutcome SYNTAX_ERROR = new CommandOutcome(Status.SYNTAX_ERROR);
    public static final CommandOutcome EXECUTION_ERROR = new CommandOutcome(Status.EXECUTION_ERROR);

    public enum Status {
        SUCCESS,
        /// The player was not allowed to run it. Indistinguishable from [#NOT_FOUND] to them, on
        /// purpose, but not here.
        DENIED,
        NOT_FOUND,
        /// It exists and they may run it, but what they typed did not parse.
        SYNTAX_ERROR,
        /// It threw. `error` carries what.
        EXECUTION_ERROR,
        UNKNOWN
    }
}
