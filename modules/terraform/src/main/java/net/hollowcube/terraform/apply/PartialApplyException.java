package net.hollowcube.terraform.apply;

/// An apply failed after writing part of its buffer. [#partial] holds the undo, change count and chunks of
/// everything written before the failure, so the caller can record it like a completed apply.
public final class PartialApplyException extends RuntimeException {
    private final ApplyEngine.ApplyResult partial;

    PartialApplyException(ApplyEngine.ApplyResult partial, RuntimeException cause) {
        super("apply failed after " + partial.changed() + " changes", cause);
        this.partial = partial;
    }

    public ApplyEngine.ApplyResult partial() {
        return partial;
    }
}
