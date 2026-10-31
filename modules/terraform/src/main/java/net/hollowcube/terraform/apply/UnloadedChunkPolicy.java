package net.hollowcube.terraform.apply;

/// What apply does when an edit targets a chunk that is not loaded. Resolved for every target chunk before
/// the first write.
public enum UnloadedChunkPolicy {
    /// Load (or generate) every unloaded target chunk, concurrently, then apply. Blocks the applying thread
    /// until all are loaded.
    LOAD,
    /// Apply nothing if any target chunk is unloaded. Checked before the first write, so a failed apply
    /// leaves the world untouched.
    FAIL,
}
