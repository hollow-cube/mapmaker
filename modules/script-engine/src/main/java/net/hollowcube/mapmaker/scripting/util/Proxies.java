package net.hollowcube.mapmaker.scripting.util;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class Proxies {

    public static @NotNull ProxyObject proxyObject(@NotNull Map<String, Object> map) {
        return ProxyObject.fromMap(map);
    }

    public static @NotNull ProxyObject freezeObject(@NotNull ProxyObject object) {
        // We delegate to the builtin proxy object to preserve the same behavior with host object unboxing.
        return new ProxyObject() {
            @Override
            public Object getMember(String key) {
                return object.getMember(key);
            }

            @Override
            public Object getMemberKeys() {
                return object.getMemberKeys();
            }

            @Override
            public boolean hasMember(String key) {
                return object.hasMember(key);
            }

            @Override
            public void putMember(String key, Value value) {
                throw new UnsupportedOperationException("putMember() not supported.");
            }
        };
    }
}
