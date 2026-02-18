package net.hollowcube.mapmaker.util.nats;

import io.nats.client.*;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.PublishAck;
import io.nats.client.impl.Headers;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import org.jetbrains.annotations.Blocking;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BiConsumer;

/// Wraps [JetStream] with otel tracing/propagation and some convenience methods.
public final class JetStreamWrapper {
    private static final HeadersReadWriter HEADERS_READ_WRITER = new HeadersReadWriter();

    private final Connection nc;
    private final JetStream jetStream;

    private final Tracer tracer;
    private final TextMapPropagator propagator;

    public JetStreamWrapper(Connection nc, OpenTelemetry otel) {
        try {
            this.nc = nc;
            this.jetStream = nc.jetStream();
            this.tracer = otel.getTracer("JetStreamWrapper", ServerRuntime.getRuntime().version());
            this.propagator = otel.getPropagators().getTextMapPropagator();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /// Publish a single message to JetStream. The payload will be serialized with GSON so must have the appropriate
    /// native image metadata and type adapters registered in {@link AbstractHttpService#GSON}.
    @Blocking
    public PublishAck publish(String subject, Object payload) {
        Span span = tracer.spanBuilder("JetStream publish " + subject)
            .setSpanKind(SpanKind.PRODUCER)
            .setAttribute("messaging.system", "nats")
            .setAttribute("messaging.destination", subject)
            .startSpan();

        try (var _ = span.makeCurrent()) {
            Headers headers = new Headers();
            propagator.inject(Context.current(), headers, HEADERS_READ_WRITER);

            final byte[] rawPayload = AbstractHttpService.GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);

            return jetStream.publish(subject, headers, rawPayload);
        } catch (IOException | JetStreamApiException e) {
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    public <T> MessageConsumer subscribe(String stream, ConsumerConfiguration config, Class<T> messageType, BiConsumer<Message, T> handler) {
        try {
            final StreamContext streamContext = nc.getStreamContext(stream);
            final ConsumerContext consumer = streamContext.createOrUpdateConsumer(config);
            return consumer.consume(msg -> {
                Headers headers = msg.getHeaders();
                Context extractedContext = propagator.extract(Context.current(), headers, HEADERS_READ_WRITER);

                Span span = tracer.spanBuilder("JetStream consume " + msg.getSubject())
                    .setParent(extractedContext)
                    .setAttribute("messaging.system", "nats")
                    .setAttribute("messaging.destination", msg.getSubject())
                    .setSpanKind(SpanKind.CONSUMER)
                    .startSpan();

                try (var _ = span.makeCurrent()) {
                    final String rawData = new String(msg.getData(), StandardCharsets.UTF_8);
                    final T data = AbstractHttpService.GSON.fromJson(rawData, messageType);
                    handler.accept(msg, data);
                } catch (Exception e) {
                    span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
                    span.recordException(e);
                } finally {
                    span.end();
                }
            });
        } catch (IOException | JetStreamApiException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class HeadersReadWriter implements TextMapGetter<Headers>, TextMapSetter<Headers> {
        @Override
        public Iterable<String> keys(@Nullable Headers carrier) {
            return carrier != null ? carrier.keySet() : List.of();
        }

        @Override
        public @Nullable String get(@Nullable Headers carrier, String key) {
            return carrier != null ? carrier.getFirst(key) : null;
        }

        @Override
        public void set(@Nullable Headers carrier, String key, String value) {
            if (carrier == null) return;
            carrier.put(key, value);
        }
    }
}
