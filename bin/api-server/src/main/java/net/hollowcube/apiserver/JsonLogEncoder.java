package net.hollowcube.apiserver;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.encoder.EncoderBase;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/// One flat json object per line, shaped like the Go api-server's zap output so that both halves of
/// the port read the same in Loki: `level`, `ts`, `msg`, then whatever the call site attached.
///
/// Hand written rather than logback's own [ch.qos.logback.classic.encoder.JsonEncoder], which nests
/// key-values under a `kvpList` array of single-key objects and stringifies every value — LogQL
/// would see `kvpList_0_status` as `"503"` rather than a `status` it can compare numerically. It is
/// also why there is no logstash encoder here: this is a page of code against a dependency whose
/// reflection would have to be described to native-image.
public final class JsonLogEncoder extends EncoderBase<ILoggingEvent> {
    private static final byte[] EMPTY = new byte[0];

    @Override
    public byte[] headerBytes() {
        return EMPTY;
    }

    @Override
    public byte[] footerBytes() {
        return EMPTY;
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        var out = new StringWriter();
        try (var json = new JsonWriter(out)) {
            json.beginObject();
            // zap's field names, so a Loki query can span the go server and this one.
            json.name("level").value(event.getLevel().toString().toLowerCase());
            // BigDecimal, not a double: Double.toString would render this as 1.78E9, and zap
            // writes plain epoch seconds.
            json.name("ts").value(BigDecimal.valueOf(event.getTimeStamp()).movePointLeft(3));
            json.name("logger").value(event.getLoggerName());
            json.name("thread").value(event.getThreadName());
            json.name("msg").value(event.getFormattedMessage());

            var keyValues = event.getKeyValuePairs();
            if (keyValues != null) {
                for (var pair : keyValues) {
                    json.name(pair.key);
                    switch (pair.value) {
                        // Numbers and booleans go out unquoted, which is the whole point: it is what
                        // lets `| json | status >= 500` work without unwrapping a string first.
                        case null -> json.nullValue();
                        case Number number -> json.value(number);
                        case Boolean bool -> json.value(bool);
                        default -> json.value(String.valueOf(pair.value));
                    }
                }
            }

            IThrowableProxy throwable = event.getThrowableProxy();
            if (throwable != null) json.name("err").value(ThrowableProxyUtil.asString(throwable));

            json.endObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e); // a StringWriter does not do this
        }
        return (out + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
    }
}
