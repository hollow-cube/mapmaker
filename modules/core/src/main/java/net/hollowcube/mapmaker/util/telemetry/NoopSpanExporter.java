package net.hollowcube.mapmaker.util.telemetry;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.Collection;

public class NoopSpanExporter implements SpanExporter {
    public static final NoopSpanExporter INSTANCE = new NoopSpanExporter();

    private NoopSpanExporter() {
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }
}
