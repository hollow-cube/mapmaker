package net.hollowcube.ipc.util;

import com.sun.net.httpserver.HttpExchange;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.semconv.SemanticAttributes;
import net.hollowcube.ipc.Wire;
import org.jetbrains.annotations.Nullable;

import java.net.http.HttpRequest;

/// The tracing both halves of a generated ipc service do, so that neither of them has to spell it
/// out and the two cannot disagree about how a call is named.
///
/// A client span and the server span it caused are one trace: the client writes the propagation
/// headers onto the request, and the server reads them back off the exchange. Handed
/// [OpenTelemetry#noop] — which is what a caller that passes nothing gets — every one of these is a
/// no-op that allocates nothing worth caring about.
public final class IpcTracing {

    /// The caller's [Wire#clientVersion], as the server span records it.
    public static final AttributeKey<String> CLIENT_VERSION = AttributeKey.stringKey("ipc.client.version");

    private static final TextMapSetter<HttpRequest.Builder> SETTER = (carrier, key, value) -> {
        if (carrier != null) carrier.header(key, value);
    };

    private static final TextMapGetter<HttpExchange> GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(HttpExchange exchange) {
            return exchange.getRequestHeaders().keySet();
        }

        @Override
        public @Nullable String get(@Nullable HttpExchange exchange, String key) {
            if (exchange == null) return null;
            var values = exchange.getRequestHeaders().get(key);
            return values == null || values.isEmpty() ? null : values.getFirst();
        }
    };

    private final OpenTelemetry otel;
    private final Tracer tracer;
    private final String service;

    /// @param service the service's route without its slash, which is what its spans are named after
    public IpcTracing(OpenTelemetry otel, String service) {
        this.otel = otel;
        this.tracer = otel.getTracer("net.hollowcube.ipc");
        this.service = service;
    }

    /// Starts the span for one outgoing call, and writes the propagation headers onto `request`.
    public IpcSpan client(String method, HttpRequest.Builder request, String url) {
        var span = tracer.spanBuilder(service + "/" + method)
            .setSpanKind(SpanKind.CLIENT)
            .startSpan();
        var scope = span.makeCurrent();
        otel.getPropagators().getTextMapPropagator().inject(Context.current(), request, SETTER);
        span.setAttribute(SemanticAttributes.HTTP_REQUEST_METHOD, "POST");
        span.setAttribute(SemanticAttributes.URL_FULL, url);
        return new IpcSpan(span, scope);
    }

    /// Starts the span for one incoming call, continuing the trace the caller propagated.
    public IpcSpan server(String method, HttpExchange exchange) {
        var caller = otel.getPropagators().getTextMapPropagator()
            .extract(Context.current(), exchange, GETTER);
        var span = tracer.spanBuilder(service + "/" + method)
            .setSpanKind(SpanKind.SERVER)
            .setParent(caller)
            .startSpan();
        span.setAttribute(SemanticAttributes.HTTP_REQUEST_METHOD, exchange.getRequestMethod());
        span.setAttribute(SemanticAttributes.URL_PATH, exchange.getRequestURI().getPath());
        var client = exchange.getRequestHeaders().getFirst(Wire.CLIENT_HEADER);
        if (client != null) span.setAttribute(CLIENT_VERSION, client);
        return new IpcSpan(span, span.makeCurrent());
    }
}
