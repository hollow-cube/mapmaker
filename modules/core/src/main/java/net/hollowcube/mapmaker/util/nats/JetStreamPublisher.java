package net.hollowcube.mapmaker.util.nats;

import io.nats.client.api.PublishAck;
import io.nats.client.impl.Headers;
import org.jetbrains.annotations.Blocking;

/// Both return once the stream has stored the message.
public interface JetStreamPublisher {

    /// `payload` is serialized with [net.hollowcube.mapmaker.util.AbstractHttpService#GSON].
    @Blocking
    PublishAck publish(String subject, Object payload);

    @Blocking
    PublishAck publish(String subject, Headers headers, byte[] data);
}
