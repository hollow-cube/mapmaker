package net.hollowcube.apiserver.common;

import com.google.gson.Gson;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.impl.Headers;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/// What an api process says on NATS: a core publish of one record, encoded with the gson its
/// readers decode it with.
///
/// Core rather than JetStream because these are events, not work — a server that was not listening
/// when a message went out has nothing to catch up on, and a stream that stores them would only be
/// a queue nobody drains. JetStream still captures a core publish onto any stream whose subjects
/// match, which is what lets the legacy chat subject keep reaching servers consuming it as a
/// stream.
public final class NatsPublisher implements AutoCloseable {
    private static final TextMapSetter<Headers> SETTER = (carrier, key, value) -> {
        if (carrier != null) carrier.put(key, value);
    };

    private final Connection nc;
    private final Gson gson;
    private final Tracer tracer;
    private final TextMapPropagator propagator;

    /// Connects, and keeps trying in the background if it cannot.
    ///
    /// A broker that is briefly unreachable at startup is something to report through the readiness
    /// probe rather than to crash over — the same call the pools make — and the rest of what this
    /// process serves does not need it at all.
    ///
    /// @param servers comma separated, as the `nats.servers` vault key spells them
    public static NatsPublisher connect(String servers, Gson gson) {
        try {
            return new NatsPublisher(Nats.connectReconnectOnConnect(Options.builder()
                .servers(servers.split(","))
                .maxReconnects(-1)
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build()), gson);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new IllegalStateException("failed to connect to nats at " + servers, e);
        }
    }

    public NatsPublisher(Connection nc, Gson gson) {
        this(nc, gson, OpenTelemetry.noop());
    }

    public NatsPublisher(Connection nc, Gson gson, OpenTelemetry otel) {
        this.nc = nc;
        this.gson = gson;
        this.tracer = otel.getTracer("net.hollowcube.apiserver.nats");
        this.propagator = otel.getPropagators().getTextMapPropagator();
    }

    /// Publishes `message` on `subject`, carrying the current trace across to whoever reads it.
    ///
    /// Does not block on the server acknowledging anything — a core publish is fire and forget, and
    /// a caller who needed to know it landed would want a stream instead.
    public void publish(String subject, Object message) {
        var span = tracer.spanBuilder("NATS publish " + subject)
            .setSpanKind(SpanKind.PRODUCER)
            .setAttribute("messaging.system", "nats")
            .setAttribute("messaging.destination", subject)
            .startSpan();
        try (var _ = span.makeCurrent()) {
            var headers = new Headers();
            propagator.inject(Context.current(), headers, SETTER);
            nc.publish(subject, headers, gson.toJson(message).getBytes(StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            span.setStatus(StatusCode.ERROR, String.valueOf(e.getMessage()));
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /// Whether the connection is up, for the readiness probe. A reconnecting client is not ready:
    /// anything published while it is down is lost, and nothing here retries.
    public boolean connected() {
        return nc.getStatus() == Connection.Status.CONNECTED;
    }

    @Override
    public void close() {
        try {
            nc.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
