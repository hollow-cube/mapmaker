package net.hollowcube.apiserver.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.nats.client.Connection;
import net.hollowcube.ipc.Wire;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// A [NatsPublisher] over a connection that keeps what was published instead of sending it.
public final class RecordingNats {

    public final List<Map.Entry<String, String>> sent = new ArrayList<>();
    public final NatsPublisher publisher;

    public RecordingNats() {
        var connection = (Connection) Proxy.newProxyInstance(
            RecordingNats.class.getClassLoader(),
            new Class<?>[] {Connection.class},
            (_, method, args) -> {
                if (method.getName().equals("publish") && args != null && args.length == 3) {
                    sent.add(
                        Map.entry(
                            (String) args[0],
                            new String((byte[]) args[2], StandardCharsets.UTF_8)
                        )
                    );
                    return null;
                }
                return method.getReturnType().isPrimitive() ? false : null;
            }
        );
        publisher = new NatsPublisher(connection, Wire.gson());
    }

    public List<JsonElement> on(String subject) {
        return sent.stream()
            .filter(entry -> entry.getKey().equals(subject))
            .map(entry -> JsonParser.parseString(entry.getValue()))
            .toList();
    }
}
