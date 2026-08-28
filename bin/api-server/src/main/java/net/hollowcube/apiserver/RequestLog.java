package net.hollowcube.apiserver;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/// One line per request, carrying what the Go api-server's `ZapMiddleware` carries: proto, method,
/// path, latency, status, response size, and the trace and user agent when there are any.
///
/// A [Filter] rather than a wrapper around each handler, because the status and the response size
/// are only known once the handler has written them, and this is where the jdk server lets us look.
public final class RequestLog extends Filter {
    private static final Logger logger = LoggerFactory.getLogger(RequestLog.class);

    /// `traceparent` is `version-traceid-spanid-flags`; the id is the only field worth a log line,
    /// and reading it here rather than from the span means one format for every handler, traced or
    /// not. A request that arrives without the header started its trace inside this process, so
    /// there is nothing to correlate to yet and the field is left off.
    private static final int TRACEPARENT_FIELDS = 4;
    private static final int TRACE_ID_FIELD = 1;

    @Override
    public String description() {
        return "logs one line per request";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        var start = System.nanoTime();
        var body = new CountingOutputStream(exchange.getResponseBody());
        exchange.setStreams(null, body);
        try {
            chain.doFilter(exchange);
        } finally {
            log(exchange, body.written, System.nanoTime() - start);
        }
    }

    private static void log(HttpExchange exchange, long size, long elapsedNanos) {
        var path = exchange.getRequestURI().getPath();
        var status = exchange.getResponseCode();

        // A probe answering the way it is supposed to is not news, and kubelet asks constantly. One
        // that fails is exactly what someone reading these logs is looking for.
        if (status == 200 && (path.endsWith("/alive") || path.endsWith("/ready"))) return;

        var message = new StringBuilder("served ")
            .append(exchange.getProtocol()).append(' ')
            .append(exchange.getRequestMethod()).append(' ')
            .append(path)
            .append(" status=").append(status)
            .append(" size=").append(size)
            .append(" lat=").append(TimeUnit.NANOSECONDS.toMillis(elapsedNanos)).append("ms");

        var trace = traceId(exchange);
        if (trace != null) message.append(" trace=").append(trace);

        var userAgent = exchange.getRequestHeaders().getFirst("User-Agent");
        if (userAgent != null && !userAgent.isBlank()) message.append(" ua=").append(userAgent);

        logger.info(message.toString());
    }

    private static String traceId(HttpExchange exchange) {
        var traceparent = exchange.getRequestHeaders().getFirst("traceparent");
        if (traceparent == null) return null;
        var fields = traceparent.split("-");
        return fields.length == TRACEPARENT_FIELDS ? fields[TRACE_ID_FIELD] : null;
    }

    /// The jdk server does not report how much a handler wrote, so this counts it on the way past.
    private static final class CountingOutputStream extends FilterOutputStream {
        private long written;

        private CountingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            written++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            // FilterOutputStream#write(byte[], int, int) writes a byte at a time through write(int)
            // by default, which would be slow and would count correctly for the wrong reason.
            out.write(b, off, len);
            written += len;
        }
    }
}
