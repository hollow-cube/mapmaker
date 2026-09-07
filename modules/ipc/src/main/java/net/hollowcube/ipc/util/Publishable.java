package net.hollowcube.ipc.util;

/// A record that knows the NATS subject it goes out on, so a publisher takes the record alone.
public interface Publishable {
    String subject();
}
