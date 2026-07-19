package net.hollowcube.mapmaker.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class PendingPlayerTrackerTest {
    @Test
    void releasesWorldOnlyAfterLastPendingPlayer() {
        var tracker = new PendingPlayerTracker<Object, Object>();
        var world = new Object();
        var firstPlayer = new Object();
        var secondPlayer = new Object();

        assertTrue(tracker.reserve(world, firstPlayer));
        assertTrue(tracker.reserve(world, secondPlayer));
        assertTrue(tracker.hasPendingPlayers(world));

        assertNull(tracker.release(firstPlayer));
        assertTrue(tracker.hasPendingPlayers(world));

        assertSame(world, tracker.release(secondPlayer));
        assertFalse(tracker.hasPendingPlayers(world));
    }

    @Test
    void doesNotMoveAnExistingReservationToAnotherWorld() {
        var tracker = new PendingPlayerTracker<Object, Object>();
        var firstWorld = new Object();
        var secondWorld = new Object();
        var player = new Object();

        assertTrue(tracker.reserve(firstWorld, player));
        assertTrue(tracker.reserve(firstWorld, player));
        assertFalse(tracker.reserve(secondWorld, player));

        assertSame(firstWorld, tracker.release(player));
        assertFalse(tracker.hasPendingPlayers(firstWorld));
        assertFalse(tracker.hasPendingPlayers(secondWorld));
    }
}
