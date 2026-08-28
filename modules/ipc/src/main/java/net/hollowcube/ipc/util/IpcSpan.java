package net.hollowcube.ipc.util;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.SemanticAttributes;

/// One ipc call's span, current for as long as it is open.
///
/// Closing it closes the scope before ending the span, so anything the call does downstream — the
/// far side's server span, a query, another ipc hop — lands underneath this one.
public final class IpcSpan implements AutoCloseable {
    private final Span span;
    private final Scope scope;

    IpcSpan(Span span, Scope scope) {
        this.span = span;
        this.scope = scope;
    }

    /// Records the status the call answered with. Anything outside 2xx also ends the span in error,
    /// which is what makes a failing ipc route visible without reading every span's attributes.
    public void status(int status) {
        span.setAttribute(SemanticAttributes.HTTP_RESPONSE_STATUS_CODE, (long) status);
        if (status < 200 || status >= 300) span.setStatus(StatusCode.ERROR);
    }

    /// Records a call that never got a status at all.
    public void failed(Throwable thrown) {
        span.recordException(thrown);
        span.setStatus(StatusCode.ERROR, String.valueOf(thrown.getMessage()));
    }

    @Override
    public void close() {
        scope.close();
        span.end();
    }
}
