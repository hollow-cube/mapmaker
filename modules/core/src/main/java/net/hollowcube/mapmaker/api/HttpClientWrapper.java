package net.hollowcube.mapmaker.api;

import com.google.gson.reflect.TypeToken;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static net.hollowcube.mapmaker.util.AbstractHttpService.CONTEXT_PROPAGATOR;

public class HttpClientWrapper {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientWrapper.class);
    private final HttpClient httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private final OpenTelemetry otel;
    private final Tracer tracer;
    private final String baseUrl;

    public HttpClientWrapper(OpenTelemetry otel, String baseUrl) {
        this.otel = otel;
        this.tracer = otel.getTracer(getClass().getName(), ServerRuntime.getRuntime().version());
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public <T> T get(String operation, String url, TypeToken<T> responseType) {
        var res = doRequest(operation,
            HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + url))
                .GET(),
            HttpResponse.BodyHandlers.ofString());
        return parseOrThrowResponse(res, responseType);
    }

    public void post(String operation, String url) {
        var res = doRequest(operation,
            HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + url))
                .POST(HttpRequest.BodyPublishers.noBody()),
            HttpResponse.BodyHandlers.ofString());
        maybeThrowResponse(res);
    }

    public void post(String operation, String url, Object requestBody) {
        var body = AbstractHttpService.GSON.toJson(requestBody);
        var res = doRequest(operation,
            HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + url))
                .POST(HttpRequest.BodyPublishers.ofString(body)),
            HttpResponse.BodyHandlers.ofString());
        maybeThrowResponse(res);
    }

    public <T> T post(String operation, String url, Object requestBody, TypeToken<T> responseType) {
        var body = AbstractHttpService.GSON.toJson(requestBody);
        var res = doRequest(operation,
            HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + url))
                .POST(HttpRequest.BodyPublishers.ofString(body)),
            HttpResponse.BodyHandlers.ofString());
        return parseOrThrowResponse(res, responseType);
    }

    public void patch(String operation, String url, Object requestBody) {
        var body = AbstractHttpService.GSON.toJson(requestBody);
        var res = doRequest(operation,
            HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + url))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body)),
            HttpResponse.BodyHandlers.ofString());
        maybeThrowResponse(res);
    }

    public <T> T patch(String operation, String url, Object requestBody, TypeToken<T> responseType) {
        var body = AbstractHttpService.GSON.toJson(requestBody);
        var res = doRequest(operation,
            HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + url))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body)),
            HttpResponse.BodyHandlers.ofString());
        return parseOrThrowResponse(res, responseType);
    }

    public void put(String operation, String url, Object requestBody) {
        var body = AbstractHttpService.GSON.toJson(requestBody);
        var res = doRequest(operation,
            HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + url))
                .PUT(HttpRequest.BodyPublishers.ofString(body)),
            HttpResponse.BodyHandlers.ofString());
        maybeThrowResponse(res);
    }

    public <T> T put(String operation, String url, Object requestBody, TypeToken<T> responseType) {
        var body = AbstractHttpService.GSON.toJson(requestBody);
        var res = doRequest(operation,
            HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + url))
                .PUT(HttpRequest.BodyPublishers.ofString(body)),
            HttpResponse.BodyHandlers.ofString());
        return parseOrThrowResponse(res, responseType);
    }

    public void delete(String operation, String url) {
        var res = doRequest(operation,
            HttpRequest.newBuilder()
                .DELETE().uri(java.net.URI.create(baseUrl + url)),
            HttpResponse.BodyHandlers.ofString());
        maybeThrowResponse(res);
    }

    public <T> HttpResponse<T> doRequest(String name, HttpRequest.Builder reqBuilder, HttpResponse.BodyHandler<T> handler) {
        FutureUtil.assertThreadWarn();
        var span = tracer.spanBuilder(name).setSpanKind(SpanKind.CLIENT).startSpan();
        var context = Context.root().with(span);
        try {
            // Append propagation headers to the request
            otel.getPropagators().getTextMapPropagator().inject(context, reqBuilder, CONTEXT_PROPAGATOR);
            var req = reqBuilder.build();

            span.setAttribute(HttpAttributes.HTTP_REQUEST_METHOD, req.method());
            span.setAttribute(UrlAttributes.URL_FULL, req.uri().toString());

            logger.debug("{} {}", req.method(), req.uri());
            return httpClient.send(req, handler);
        } catch (InterruptedException e) {
            // propagate the interrupted exception without declaring it. We want to be able to handle
            // this, eg in a gui to abort load. But its a pain to handle it everywhere we dont care and
            // will end up propagating it manually anyway.
            return sneakyThrow(e);
        } catch (IOException e) {
            throw new RuntimeException("http request failed", e);
        } finally {
            span.end();
        }
    }

    public <T> T parseOrThrowResponse(HttpResponse<String> response, TypeToken<T> responseType) {
        return switch (response.statusCode()) {
            case 200, 201, 204 -> AbstractHttpService.GSON.fromJson(response.body(), responseType);
            default -> maybeThrowResponse(response);
        };
    }

    public <T> @UnknownNullability T maybeThrowResponse(HttpResponse<String> response) {
        return switch (response.statusCode()) {
            case 200, 201, 204 -> null;
            case 400 -> throw new ApiClient.BadRequestError(response);
            case 404 -> throw new ApiClient.NotFoundError(response);
            case 500 -> throw new ApiClient.InternalServerError(response);
            // ... more specific handlers as needed
            default -> throw new ApiClient.Error(response);
        };
    }

    public static String query(@Nullable Object... pairs) {
        if (pairs.length % 2 != 0)
            throw new IllegalArgumentException("Must have an even number of arguments");
        var builder = new StringBuilder().append('?');
        for (int i = 0; i < pairs.length; i += 2) {
            var value = pairs[i + 1];
            if (value == null) continue; // skip null values entirely

            builder.append(pairs[i]).append('=').append(urlEncode(value.toString()));
            if (i != pairs.length - 2) builder.append('&');
        }
        return builder.toString();
    }

    public static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
}
