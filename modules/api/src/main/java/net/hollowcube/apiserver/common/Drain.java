package net.hollowcube.apiserver.common;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

/// The first half of a shutdown a rolling update can live with. Kubernetes takes a terminating pod
/// out of its Service, but a caller holding a pooled keep-alive connection never hears about that
/// and keeps sending to it, and `HttpServer.stop` neither serves those requests nor tells the
/// caller to leave: it cuts the connection when its delay runs out, which the caller sees as an
/// EOF with no response. While draining, the readiness probe fails and every answer carries
/// `Connection: close`, which the jdk server honours by closing the connection after the exchange,
/// so a caller's next request opens a new connection against whatever pod is ready by then.
public final class Drain extends Filter {
    private volatile boolean draining;

    public boolean draining() {
        return draining;
    }

    public void begin() {
        draining = true;
    }

    @Override
    public String description() {
        return "closes connections while draining";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        if (draining) exchange.getResponseHeaders().set("Connection", "close");
        chain.doFilter(exchange);
    }
}
