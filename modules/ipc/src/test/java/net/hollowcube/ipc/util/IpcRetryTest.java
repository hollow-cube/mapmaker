package net.hollowcube.ipc.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IpcRetryTest {

    @Test
    void run_repeatsAnUnansweredOrServerFailure() {
        var calls = failing(
            new IpcException(0, "/map/get", "refused", new IOException()),
            IpcException.unavailable("draining")
        );
        IpcRetry.run(calls::next);
        assertEquals(3, calls.count);
    }

    @Test
    void run_takesAConflictOnARepeatAsTheEarlierAttemptLanding() {
        var calls = failing(
            IpcException.internal("lost the answer"),
            IpcException.conflict("already done")
        );
        IpcRetry.run(calls::next);
        assertEquals(2, calls.count);
    }

    @Test
    void run_givesUpOnARefusalAtOnce() {
        var refused = IpcException.conflict("already done");
        var calls = failing(refused);
        assertSame(refused, assertThrows(IpcException.class, () -> IpcRetry.run(calls::next)));
        assertEquals(1, calls.count);
    }

    @Test
    void isTransient_excludesAnInterruptedCall() {
        assertFalse(
            new IpcException(0, "/map/get", "interrupted", new InterruptedException()).isTransient()
        );
        assertTrue(IpcException.internal("boom").isTransient());
        assertFalse(IpcException.badRequest("bad").isTransient());
    }

    private static Calls failing(IpcException... failures) {
        return new Calls(new ArrayDeque<>(List.of(failures)));
    }

    private static final class Calls {
        private final ArrayDeque<IpcException> failures;
        int count;

        Calls(ArrayDeque<IpcException> failures) {
            this.failures = failures;
        }

        void next() {
            count++;
            var failure = failures.poll();
            if (failure != null) throw failure;
        }
    }
}
