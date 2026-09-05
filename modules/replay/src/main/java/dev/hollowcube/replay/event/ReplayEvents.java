package dev.hollowcube.replay.event;

/// The generic events that every replay understands, and that playback knows how to apply.
///
/// These occupy the low end of the event ID space. A host starts from [#builder()] and appends its
/// own events after them, so that two hosts recording the same generic actions produce compatible
/// replays and only diverge where they genuinely differ.
public final class ReplayEvents {

    private ReplayEvents() {
    }

    /// A registry builder pre-populated with the built-in events.
    ///
    /// WARNING: It is never valid to alter the order of these registrations under any
    /// circumstance. Event IDs are positional and are baked into every replay ever recorded.
    public static ReplayEventRegistry.Builder builder() {
        // Ordered by how often a recording carries them, since an ID is a varint and the common
        // events may as well be the cheap ones.
        return ReplayEventRegistry.builder()
            .register(DeltaMoveEvent.class, DeltaMoveEvent.NETWORK_TYPE)
            .register(AbsoluteMoveEvent.class, AbsoluteMoveEvent.NETWORK_TYPE)
            .register(EntityStateEvent.class, EntityStateEvent.NETWORK_TYPE)
            .register(HandAnimationEvent.class, HandAnimationEvent.NETWORK_TYPE)
            .register(ItemUseEvent.class, ItemUseEvent.NETWORK_TYPE)
            .register(SetItemEvent.class, SetItemEvent.NETWORK_TYPE, SetItemEvent::skip)
            .register(ChangeHeldSlotEvent.class, ChangeHeldSlotEvent.NETWORK_TYPE)
            .register(SetBlockEvent.class, SetBlockEvent.NETWORK_TYPE)
            .register(SpawnEntityEvent.class, SpawnEntityEvent.NETWORK_TYPE)
            .register(DestroyEntityEvent.class, DestroyEntityEvent.NETWORK_TYPE);
    }
}
